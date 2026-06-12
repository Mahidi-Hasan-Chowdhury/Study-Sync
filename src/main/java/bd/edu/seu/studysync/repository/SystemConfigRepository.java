package bd.edu.seu.studysync.repository;

import bd.edu.seu.studysync.model.SystemConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemConfigRepository extends MongoRepository<SystemConfig, String> {
}
