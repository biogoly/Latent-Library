package com.nilsson.backend.controller;

import com.nilsson.backend.model.ImageDTO;
import com.nilsson.backend.service.PathService;
import com.nilsson.backend.service.ThumbnailService;
import com.nilsson.backend.service.UserDataManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test suite for the {@link ImageController}, validating the REST API endpoints
 * for image searching, metadata retrieval, and lifecycle management.
 * <p>
 * This class utilizes {@link MockMvc} to verify the controller's request mapping and
 * response formatting logic, ensuring:
 * <ul>
 *   <li><b>Advanced Search:</b> Confirms that filtered search queries are correctly
 *   delegated to the data manager and return standardized JSON payloads.</li>
 *   <li><b>Metadata Access:</b> Validates the retrieval of image-specific metadata and
 *   the correct handling of 404 Resource Not Found scenarios.</li>
 *   <li><b>User Interactions:</b> Ensures that rating updates and file management
 *   operations (renaming, batch deletion) are correctly processed.</li>
 *   <li><b>Filter Discovery:</b> Verifies the dynamic generation of metadata filter
 *   values for the UI.</li>
 * </ul>
 */
@WebMvcTest(ImageController.class)
@ActiveProfiles("test")
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDataManager dataManager;

    @MockBean
    private PathService pathService;

    @MockBean
    private ThumbnailService thumbnailService;

    @MockBean
    private DataSource dataSource;

    @Test
    @DisplayName("GET /api/images/search should return JSON list of images")
    void searchImages_ShouldReturnJsonList() throws Exception {
        when(dataManager.findFilesWithFilters(any(), any(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of(new ImageDTO("/path.png", 0, ""))));

        mockMvc.perform(get("/api/images/search")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/images/filters should return distinct metadata values")
    void getFilters_ShouldReturnFilterMap() throws Exception {
        when(dataManager.getDistinctMetadataValues(anyString())).thenReturn(List.of("Val1", "Val2"));

        mockMvc.perform(get("/api/images/filters"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/images/rating should update image rating")
    void setRating_ShouldUpdateRating() throws Exception {
        Path tempFile = Files.createTempFile("test-rating", ".png");
        try {
            when(pathService.resolve(anyString())).thenReturn(tempFile.toFile());

            mockMvc.perform(post("/api/images/rating")
                            .param("path", "test-rating.png")
                            .param("rating", "5"))
                    .andExpect(status().isOk());

            verify(dataManager).setRating(any(File.class), eq(5));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("GET /api/images/metadata should return 404 for non-existent files")
    void getMetadata_NotFound_ShouldReturn404() throws Exception {
        File missingFile = new File("non-existent-file.png");
        when(pathService.resolve(anyString())).thenReturn(missingFile);

        mockMvc.perform(get("/api/images/metadata")
                        .param("path", "missing.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/images/content should return 404 when the entry exists but cannot be opened")
    void getImageContent_StatOkButUnreadable_ShouldReturn404(@TempDir Path tempDir) throws Exception {
        // A directory stats as existing but cannot be opened for reading - the same shape as the
        // "ghost" entries seen on disconnected/offline drives, where exists() and length() succeed
        // but opening the file fails. Serving these committed a 200 with a Content-Length whose body
        // never arrived, hanging the connection until the client timed out.
        when(pathService.resolve(anyString())).thenReturn(tempDir.toFile());

        mockMvc.perform(get("/api/images/content")
                        .param("path", tempDir.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/images/thumbnail should return 404 when the entry exists but cannot be opened")
    void getThumbnail_StatOkButUnreadable_ShouldReturn404(@TempDir Path tempDir) throws Exception {
        when(pathService.resolve(anyString())).thenReturn(tempDir.toFile());

        mockMvc.perform(get("/api/images/thumbnail")
                        .param("path", tempDir.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/images/content should still serve a genuinely readable file")
    void getImageContent_ReadableFile_ShouldReturnOk(@TempDir Path tempDir) throws Exception {
        Path image = tempDir.resolve("real.png");
        Files.write(image, new byte[]{1, 2, 3, 4});
        when(pathService.resolve(anyString())).thenReturn(image.toFile());

        mockMvc.perform(get("/api/images/content")
                        .param("path", image.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/images/batch/delete should invoke batch deletion logic")
    void batchDeleteImages_ShouldInvokeService() throws Exception {
        String json = "[\"/path/1.png\", \"/path/2.png\"]";

        mockMvc.perform(post("/api/images/batch/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(dataManager).batchDeleteFiles(anyList());
    }
}
