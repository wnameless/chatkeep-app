package me.moonote.app.chatkeep.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import me.moonote.app.chatkeep.model.Label;

@Repository
public interface LabelRepository extends MongoRepository<Label, String> {

  /**
   * Find all labels owned by a specific user.
   *
   * @param userId User ID
   * @return List of labels
   */
  List<Label> findByUserId(String userId);

  /**
   * Find a label by user ID and normalized name (case-insensitive lookup).
   *
   * @param userId User ID
   * @param normalizedName Lowercase label name
   * @return Optional Label if found
   */
  Optional<Label> findByUserIdAndNormalizedName(String userId, String normalizedName);

  /**
   * Check if a label with the given normalized name already exists for a user (case-insensitive
   * check).
   *
   * @param userId User ID
   * @param normalizedName Lowercase label name
   * @return true if label exists
   */
  boolean existsByUserIdAndNormalizedName(String userId, String normalizedName);

}
