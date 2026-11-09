package com.threadflow.production;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ProductionPhotoStorage {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final Path root;

    public ProductionPhotoStorage(
            @Value("${threadflow.production.photo-storage-root:${java.io.tmpdir}/threadflow-production-photos}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredPhoto store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Photo file is empty");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("Photo exceeds the 10 MB limit");
        String contentType = file.getContentType();
        if (!TYPES.contains(contentType)) throw new IllegalArgumentException("Unsupported photo content type: " + contentType);
        try {
            byte[] bytes = file.getBytes();
            if (!looksLikeSupportedImage(bytes)) throw new IllegalArgumentException("Photo content does not match JPEG, PNG or WebP");
            Files.createDirectories(root);
            String extension = switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                default -> ".webp";
            };
            String storedName = UUID.randomUUID() + extension;
            Path target = root.resolve(storedName).normalize();
            Files.write(target, bytes);
            return new StoredPhoto(storedName, target.toString(), file.getOriginalFilename(), contentType, file.getSize());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Photo could not be stored", exception);
        }
    }

    public byte[] read(String storagePath) {
        try { return Files.readAllBytes(Path.of(storagePath)); }
        catch (IOException exception) { throw new IllegalArgumentException("Photo could not be read"); }
    }

    public org.springframework.core.io.Resource loadAsResource(String storagePath) {
        return new org.springframework.core.io.FileSystemResource(Path.of(storagePath));
    }

    public void delete(String storagePath) {
        try { Files.deleteIfExists(Path.of(storagePath)); }
        catch (IOException ignored) { }
    }

    private boolean looksLikeSupportedImage(byte[] bytes) {
        if (bytes.length < 12) return false;
        boolean jpeg = bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
        boolean png = bytes[0] == (byte)0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        boolean webp = bytes[0]=='R' && bytes[1]=='I' && bytes[2]=='F' && bytes[3]=='F'
                && bytes[8]=='W' && bytes[9]=='E' && bytes[10]=='B' && bytes[11]=='P';
        return jpeg || png || webp;
    }

    public record StoredPhoto(String storedFilename, String storagePath, String originalFilename, String contentType, long size) {}
}
