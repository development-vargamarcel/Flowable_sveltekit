package com.demo.bpm.repository;

import com.demo.bpm.entity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, String> {
}
