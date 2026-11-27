package kz.agromarket.common.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import java.io.InputStream;
import java.util.UUID;
import kz.agromarket.common.DomainException;

/**
 * ADAPTER: MinIO SDK → FileStorage.
 *
 * MinIO — S3-үйлесімді файл қоймасы (docker-compose-тағы minio сервисі).
 * Файлдар «bucket» (себет) ішінде кілт бойынша сақталады: products/…jpg, documents/…pdf.
 * Браузер MinIO-ға тікелей бармайды — файлды backend өзі оқып береді (/api/files/...),
 * сондықтан MinIO сырттан жабық қала береді.
 */
public class MinioFileStorage implements FileStorage {

    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioFileStorage(MinioClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @Override
    public String store(String folder, String extension, String contentType, InputStream data, long size) {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO-ға файл жазылмады: " + key, e);
        }
    }

    @Override
    public StoredFile load(String key) {
        try {
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            InputStream data = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
            return new StoredFile(data, stat.contentType(), stat.size());
        } catch (ErrorResponseException e) {
            throw new DomainException("NOT_FOUND", "Файл табылмады", 404);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO-дан файл оқылмады: " + key, e);
        }
    }

    /** Bucket бірінші жүктеуде жасалады: қосымша іске қосылғанда MinIO әлі дайын болмауы мүмкін */
    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        bucketReady = true;
    }
}
