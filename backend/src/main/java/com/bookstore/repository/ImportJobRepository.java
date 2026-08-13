package com.bookstore.repository;

import com.bookstore.model.ImportJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ImportJobRepository extends MongoRepository<ImportJob, String> {
    Optional<ImportJob> findByIdAndSellerId(String id, String sellerId);
    Optional<ImportJob> findFirstBySellerIdAndStatusInOrderByStartedAtDesc(String sellerId, List<String> statuses);
    List<ImportJob> findByStatusIn(List<String> statuses);
}
