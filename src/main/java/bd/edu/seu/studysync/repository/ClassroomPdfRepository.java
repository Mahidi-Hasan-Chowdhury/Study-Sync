package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.ClassroomPdf;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassroomPdfRepository extends MongoRepository<ClassroomPdf, String> {

    /**
     * Find all PDFs for a specific classroom
     */
    List<ClassroomPdf> findByClassroomIdOrderByUploadedAtDesc(String classroomId);

    /**
     * Find PDF by classroom and file name (for duplicate check)
     */
    Optional<ClassroomPdf> findByClassroomIdAndFileName(String classroomId, String fileName);

    /**
     * Count PDFs in a classroom
     */
    long countByClassroomId(String classroomId);

    /**
     * Find all PDFs uploaded by a specific user
     */
    List<ClassroomPdf> findByUploadedByOrderByUploadedAtDesc(String userId);

    /**
     * Check if PDF exists in classroom
     */
    boolean existsByClassroomIdAndId(String classroomId, String pdfId);
}
