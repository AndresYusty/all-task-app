package com.task.all.repository;

import com.task.all.model.entity.ChecklistItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
}
