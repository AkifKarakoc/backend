package com.tourguide.common.util;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioUtilTest {

    @Mock
    private MinioClient minioClient;

    @Test
    void getPresignedUrl_shouldReplaceInternalEndpointWithPublicUrl() throws Exception {
        // given
        String internalUrl = "http://localhost:9000/profile-photos/file.webp?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=abc";
        String publicUrl = "http://10.0.2.2:9000";

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(internalUrl);

        MinioUtil minioUtil = new MinioUtil(minioClient, publicUrl);

        // when
        String result = minioUtil.getPresignedUrl("profile-photos", "file.webp");

        // then
        assertThat(result).isEqualTo("http://10.0.2.2:9000/profile-photos/file.webp?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=abc");
    }

    @Test
    void getPresignedUrl_shouldPreserveOriginalUrlWhenPublicUrlNotConfigured() throws Exception {
        // given
        String internalUrl = "http://localhost:9000/profile-photos/file.webp?X-Amz-Signature=abc";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn(internalUrl);

        MinioUtil minioUtil = new MinioUtil(minioClient, "");

        // when
        String result = minioUtil.getPresignedUrl("profile-photos", "file.webp");

        // then
        assertThat(result).isEqualTo(internalUrl);
    }
}
