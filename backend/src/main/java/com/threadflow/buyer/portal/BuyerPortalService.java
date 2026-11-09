package com.threadflow.buyer.portal;

import com.threadflow.buyer.Buyer;
import com.threadflow.order.OrderMilestone;
import com.threadflow.order.OrderRepository;
import com.threadflow.order.ProductionOrder;
import com.threadflow.order.warning.BuyerDelayWarning;
import com.threadflow.order.warning.BuyerDelayWarningRepository;
import com.threadflow.order.warning.BuyerDelayWarningStatus;
import com.threadflow.production.ProductionPhoto;
import com.threadflow.production.ProductionPhotoRepository;
import com.threadflow.production.ProductionPhotoStorage;
import com.threadflow.production.ProductionUpdateRepository;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuyerPortalService {
    private final OrderRepository orderRepository;
    private final ProductionPhotoRepository photoRepository;
    private final BuyerDelayWarningRepository warningRepository;
    private final ProductionUpdateRepository updateRepository;
    private final ProductionPhotoStorage photoStorage;

    public BuyerPortalService(OrderRepository orderRepository,
                              ProductionPhotoRepository photoRepository,
                              BuyerDelayWarningRepository warningRepository,
                              ProductionUpdateRepository updateRepository,
                              ProductionPhotoStorage photoStorage) {
        this.orderRepository = orderRepository;
        this.photoRepository = photoRepository;
        this.warningRepository = warningRepository;
        this.updateRepository = updateRepository;
        this.photoStorage = photoStorage;
    }

    public Buyer requireBuyer(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Buyer buyer) {
            return buyer;
        }
        throw new AccessDeniedException("Buyer portal access token required");
    }

    @Transactional(readOnly = true)
    public BuyerPortalDtos.BuyerOverviewResponse overview(Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        List<ProductionOrder> orders = orderRepository.findByBuyer_Id(buyer.getId());

        int inProgress = 0;
        int completed = 0;
        for (ProductionOrder order : orders) {
            if (order.getStatus() == ProductionOrder.Status.IN_PROGRESS) inProgress++;
            else if (order.getStatus() == ProductionOrder.Status.COMPLETED) completed++;
        }

        List<BuyerDelayWarning> confirmedWarnings = warningRepository
                .findByOrder_Buyer_IdAndStatusOrderByConfirmedAtDesc(buyer.getId(), BuyerDelayWarningStatus.CONFIRMED);

        return new BuyerPortalDtos.BuyerOverviewResponse(
                buyer.getId(),
                buyer.getBuyerName(),
                buyer.getCountry(),
                orders.size(),
                inProgress,
                completed,
                confirmedWarnings.size()
        );
    }

    @Transactional(readOnly = true)
    public List<BuyerPortalDtos.BuyerOrderSummaryResponse> orders(Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        List<ProductionOrder> orders = orderRepository.findByBuyer_Id(buyer.getId());

        return orders.stream()
                .sorted(Comparator.comparing(ProductionOrder::getExFactoryDate))
                .map(order -> {
                    int percent = calculateProgressPercent(order);
                    List<BuyerDelayWarning> warnings = warningRepository
                            .findByOrder_IdAndStatusOrderByConfirmedAtDesc(order.getId(), BuyerDelayWarningStatus.CONFIRMED);
                    List<ProductionPhoto> photos = photoRepository
                            .findByProductionUpdate_Order_IdAndApprovedForBuyerTrueOrderByCreatedAtDesc(order.getId());
                    return new BuyerPortalDtos.BuyerOrderSummaryResponse(
                            order.getId(),
                            order.getPoNumber(),
                            order.getStyleName(),
                            order.getProductDescription(),
                            order.getTotalQuantity(),
                            order.getExFactoryDate(),
                            order.getStatus().name(),
                            percent,
                            warnings.size(),
                            photos.size()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BuyerPortalDtos.BuyerOrderDetailResponse orderDetail(Long orderId, Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        ProductionOrder order = requireBuyerOrder(orderId, buyer);

        List<BuyerPortalDtos.BuyerMilestoneProgress> milestones = order.getMilestones().stream()
                .sorted(Comparator.comparingInt(OrderMilestone::getSequenceNumber))
                .map(m -> new BuyerPortalDtos.BuyerMilestoneProgress(
                        m.getId(),
                        m.getMilestoneName(),
                        m.getSequenceNumber(),
                        m.getPlannedStartDate(),
                        m.getPlannedEndDate(),
                        m.getActualCompletionDate(),
                        m.getStatus().name()
                ))
                .toList();

        List<BuyerPortalDtos.BuyerConfirmedWarningResponse> warnings = warningRepository
                .findByOrder_IdAndStatusOrderByConfirmedAtDesc(order.getId(), BuyerDelayWarningStatus.CONFIRMED).stream()
                .map(w -> new BuyerPortalDtos.BuyerConfirmedWarningResponse(
                        w.getId(),
                        w.getRiskStatus(),
                        w.getConfirmedMessage(),
                        w.getConfirmedAt()
                ))
                .toList();

        List<BuyerPortalDtos.BuyerApprovedPhotoResponse> photos = photoRepository
                .findByProductionUpdate_Order_IdAndApprovedForBuyerTrueOrderByCreatedAtDesc(order.getId()).stream()
                .map(p -> new BuyerPortalDtos.BuyerApprovedPhotoResponse(
                        p.getId(),
                        p.getOriginalFilename(),
                        p.getContentType(),
                        p.getFileSize(),
                        p.getApprovedAt()
                ))
                .toList();

        return new BuyerPortalDtos.BuyerOrderDetailResponse(
                order.getId(),
                order.getPoNumber(),
                order.getStyleName(),
                order.getProductDescription(),
                order.getTotalQuantity(),
                order.getExFactoryDate(),
                order.getStatus().name(),
                calculateProgressPercent(order),
                milestones,
                warnings,
                photos
        );
    }

    @Transactional(readOnly = true)
    public List<BuyerPortalDtos.BuyerMilestoneProgress> milestones(Long orderId, Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        ProductionOrder order = requireBuyerOrder(orderId, buyer);

        return order.getMilestones().stream()
                .sorted(Comparator.comparingInt(OrderMilestone::getSequenceNumber))
                .map(m -> new BuyerPortalDtos.BuyerMilestoneProgress(
                        m.getId(),
                        m.getMilestoneName(),
                        m.getSequenceNumber(),
                        m.getPlannedStartDate(),
                        m.getPlannedEndDate(),
                        m.getActualCompletionDate(),
                        m.getStatus().name()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BuyerPortalDtos.BuyerApprovedPhotoResponse> approvedPhotos(Long orderId, Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        requireBuyerOrder(orderId, buyer);

        return photoRepository.findByProductionUpdate_Order_IdAndApprovedForBuyerTrueOrderByCreatedAtDesc(orderId).stream()
                .map(p -> new BuyerPortalDtos.BuyerApprovedPhotoResponse(
                        p.getId(),
                        p.getOriginalFilename(),
                        p.getContentType(),
                        p.getFileSize(),
                        p.getApprovedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource loadPhotoResource(Long orderId, Long photoId, Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        requireBuyerOrder(orderId, buyer);

        ProductionPhoto photo = photoRepository.findByIdAndApprovedForBuyerTrue(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found or not approved for buyer: " + photoId));

        if (!photo.getProductionUpdate().getOrder().getId().equals(orderId)) {
            throw new AccessDeniedException("Photo does not belong to the requested order");
        }

        return photoStorage.loadAsResource(photo.getStoragePath());
    }

    @Transactional(readOnly = true)
    public ProductionPhoto getApprovedPhoto(Long orderId, Long photoId, Authentication authentication) {
        Buyer buyer = requireBuyer(authentication);
        requireBuyerOrder(orderId, buyer);

        ProductionPhoto photo = photoRepository.findByIdAndApprovedForBuyerTrue(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found or not approved for buyer: " + photoId));

        if (!photo.getProductionUpdate().getOrder().getId().equals(orderId)) {
            throw new AccessDeniedException("Photo does not belong to the requested order");
        }
        return photo;
    }

    private ProductionOrder requireBuyerOrder(Long orderId, Buyer buyer) {
        ProductionOrder order = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new AccessDeniedException("Access denied: Order belongs to another buyer");
        }
        return order;
    }

    private int calculateProgressPercent(ProductionOrder order) {
        if (order.getStatus() == ProductionOrder.Status.COMPLETED) return 100;
        if (order.getTotalQuantity() <= 0) return 0;

        OrderMilestone finalMilestone = order.getMilestones().stream()
                .max(Comparator.comparingInt(OrderMilestone::getSequenceNumber))
                .orElse(null);

        if (finalMilestone == null) return 0;

        long completedPieces = updateRepository.sumCompletedQuantityForOrderAndMilestone(order.getId(), finalMilestone.getId());
        double percent = (completedPieces * 100.0) / order.getTotalQuantity();
        return (int) Math.min(100, Math.max(0, Math.round(percent)));
    }
}
