package com.bank.feature.storage.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FileMetaRepository extends JpaRepository<FileMeta, UUID> {}
