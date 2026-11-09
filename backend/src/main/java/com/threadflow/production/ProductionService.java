package com.threadflow.production;

import com.threadflow.issue.IssueClassificationRequested;
import com.threadflow.issue.ProductionIssue;
import com.threadflow.issue.ProductionIssueRepository;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.user.User;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductionService {
    private final ProductionUpdateRepository updates;
    private final ProductionPhotoRepository photoRepository;
    private final SupervisorOrderAccessService supervisorAccess;
    private final OrderRepository orders;
    private final ProductionPhotoStorage storage;
    private final ProductionIssueRepository issues;
    private final ApplicationEventPublisher events;

    public ProductionService(ProductionUpdateRepository updates, ProductionPhotoRepository photoRepository,
                             SupervisorOrderAccessService supervisorAccess,
                             OrderRepository orders, ProductionPhotoStorage storage,
                             ProductionIssueRepository issues, ApplicationEventPublisher events) {
        this.updates = updates; this.photoRepository = photoRepository;
        this.supervisorAccess = supervisorAccess;
        this.orders = orders; this.storage = storage; this.issues = issues; this.events = events;
    }

    @Transactional(readOnly = true)
    public List<ProductionDtos.AvailableOrderResponse> availableOrders(Authentication authentication) {
        User supervisor = requireSupervisor(authentication);
        List<Long> accessibleIds = supervisorAccess.accessibleOrderIds(supervisor.getId());
        return orders.findDistinctByIdIn(accessibleIds).stream()
                .map(order -> toAvailable(order, supervisor.getId())).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionDtos.MilestoneResponse> milestones(Long orderId, Authentication authentication) {
        User supervisor = requireSupervisor(authentication);
        supervisorAccess.requireAccess(supervisor, orderId);
        ProductionOrder order = orders.findWithDetailsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return order.getMilestones().stream().map(m -> new ProductionDtos.MilestoneResponse(
                m.getId(), m.getMilestoneName(), m.getSequenceNumber(), m.getStatus().name())).toList();
    }

    @Transactional
    public ProductionDtos.UpdateResponse submit(ProductionDtos.CreateUpdateRequest request,
                                                List<MultipartFile> photos, Authentication authentication) {
        User supervisor = requireSupervisor(authentication);
        supervisorAccess.requireAccess(supervisor, request.orderId());
        ProductionUpdate existing = updates.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.getSupervisor().getId().equals(supervisor.getId())) throw new AccessDeniedException("Idempotency key belongs to another supervisor");
            return toResponse(existing);
        }
        if (request.productionDate().isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw new IllegalArgumentException("Production date cannot be in the future");
        }
        ProductionOrder order = orders.findWithDetailsById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.orderId()));
        OrderMilestone milestone = order.getMilestones().stream()
                .filter(m -> m.getId().equals(request.milestoneId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Milestone not found for this order: " + request.milestoneId()));
        if (request.completedQuantity() < 1) throw new IllegalArgumentException("Completed quantity must be positive");
        long cumulative = updates.sumCompletedQuantityForOrderAndMilestone(order.getId(), milestone.getId())
            + request.completedQuantity();
        if (cumulative > order.getTotalQuantity()) {
            throw new IllegalArgumentException("Completed quantity for this production stage exceeds the order quantity");
        }
        ProductionUpdate update = new ProductionUpdate(order, milestone, supervisor, request.productionDate(),
                request.completedQuantity(), request.issueNote(), request.idempotencyKey());
        List<ProductionPhotoStorage.StoredPhoto> storedPhotos = new ArrayList<>();
        try {
            if (photos != null) {
                for (MultipartFile file : photos) {
                    ProductionPhotoStorage.StoredPhoto stored = storage.store(file);
                    storedPhotos.add(stored);
                    ProductionPhoto photo = new ProductionPhoto(update, stored.originalFilename(), stored.storedFilename(),
                            stored.contentType(), stored.size(), stored.storagePath());
                    update.addPhoto(photo);
                }
            }
            ProductionUpdate saved = updates.save(update);
            if (request.issueNote() != null && !request.issueNote().isBlank()) {
                ProductionIssue issue = issues.save(new ProductionIssue(saved));
                try {
                    events.publishEvent(new IssueClassificationRequested(issue.getId()));
                } catch (RuntimeException ignored) {
                    // The persisted PENDING issue remains available for manual review or reclassification.
                }
            }
            return toResponse(saved);
        } catch (RuntimeException exception) {
            storedPhotos.forEach(stored -> storage.delete(stored.storagePath()));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ProductionDtos.UpdateResponse> myUpdates(Authentication authentication) {
        User supervisor = requireSupervisor(authentication);
        return updates.findBySupervisorIdOrderByProductionDateDescCreatedAtDesc(supervisor.getId()).stream()
                .filter(update -> supervisorAccess.hasAccess(supervisor.getId(), update.getOrder().getId()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionDtos.UpdateResponse> allUpdates(Authentication authentication) {
        requireMerchandiser(authentication);
        return updates.findAll().stream()
                .sorted((a,b) -> b.getProductionDate().compareTo(a.getProductionDate()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionDtos.UpdateResponse> orderUpdates(Long orderId, Authentication authentication) {
        if (!hasAuthority(authentication, "MERCHANDISER")) {
            User supervisor = requireSupervisor(authentication);
            supervisorAccess.requireAccess(supervisor, orderId);
        }
        if (!orders.existsById(orderId)) throw new IllegalArgumentException("Order not found: " + orderId);
        return updates.findByOrderIdOrderByProductionDateDescCreatedAtDesc(orderId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductionDtos.UpdateResponse> milestoneUpdates(Long milestoneId, Authentication authentication) {
        if (hasAuthority(authentication, "MERCHANDISER")) {
            return updates.findByMilestoneIdOrderByProductionDateDescCreatedAtDesc(milestoneId).stream()
                    .map(this::toResponse).toList();
        }
        User supervisor = requireSupervisor(authentication);
        return updates.findByMilestoneIdOrderByProductionDateDescCreatedAtDesc(milestoneId).stream()
            .filter(update -> supervisorAccess.hasAccess(supervisor.getId(), update.getOrder().getId()))
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductionPhoto getAuthorizedPhoto(Long photoId, Authentication authentication) {
        ProductionPhoto photo = photoRepository.findById(photoId).orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        ProductionUpdate update = photo.getProductionUpdate();
        if (hasAuthority(authentication, "MERCHANDISER")) return photo;
        if (hasAuthority(authentication, "SUPERVISOR")) {
            User supervisor = requireSupervisor(authentication);
            supervisorAccess.requireAccess(supervisor, update.getOrder().getId());
            return photo;
        }
        throw new AccessDeniedException("Insufficient role");
    }

    private User requireSupervisor(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)
                || !hasAuthority(authentication, "SUPERVISOR")) throw new AccessDeniedException("Supervisor role required");
        return user;
    }
    private void requireMerchandiser(Authentication authentication) {
        if (!hasAuthority(authentication, "MERCHANDISER")) throw new AccessDeniedException("Merchandiser role required");
    }
    private boolean hasAuthority(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    private ProductionDtos.AvailableOrderResponse toAvailable(ProductionOrder order, Long supervisorId) {
        return new ProductionDtos.AvailableOrderResponse(order.getId(), order.getPoNumber(), order.getStyleName(),
                order.getBuyer().getBuyerName(), order.getTotalQuantity(), order.getExFactoryDate(),
                order.getProductionUnit() == null ? null : new AssignmentDtos.ProductionUnitResponse(
                        order.getProductionUnit().getId(), order.getProductionUnit().getName(),
                        order.getProductionUnit().getCode(), order.getProductionUnit().getDescription(),
                        order.getProductionUnit().isActive()),
                supervisorAccess.assignmentSource(supervisorId, order),
                order.getMilestones().stream().map(m -> new ProductionDtos.MilestoneResponse(
                        m.getId(), m.getMilestoneName(), m.getSequenceNumber(), m.getStatus().name())).toList());
    }
    private ProductionDtos.UpdateResponse toResponse(ProductionUpdate update) {
        List<ProductionDtos.PhotoResponse> photos = update.getPhotos().stream()
                .map(p -> new ProductionDtos.PhotoResponse(p.getId(), p.getOriginalFilename(), p.getContentType(), p.getFileSize())).toList();
        return new ProductionDtos.UpdateResponse(update.getId(), update.getOrder().getId(), update.getOrder().getPoNumber(),
                update.getMilestone().getId(), update.getMilestone().getMilestoneName(),
                update.getSupervisor().getId(), update.getSupervisor().getEmail(), update.getProductionDate(),
                update.getCompletedQuantity(), update.getIssueNote(), update.getCreatedAt(), update.getUpdatedAt(), photos);
    }
    public byte[] photoBytes(ProductionPhoto photo) { return storage.read(photo.getStoragePath()); }
}
