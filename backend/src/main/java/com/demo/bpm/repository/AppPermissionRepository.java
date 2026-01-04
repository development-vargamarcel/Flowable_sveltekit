package com.demo.bpm.repository;

import com.demo.bpm.entity.AppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppPermissionRepository extends JpaRepository<AppPermission, String> {
}
