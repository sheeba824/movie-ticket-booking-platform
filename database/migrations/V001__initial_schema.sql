-- Initial Schema Migration - v001
-- Created: 2026-04-07
-- Description: Create initial database schema for movie ticket booking platform

-- This file contains the initial schema creation
-- For production migrations, use Flyway or Liquibase

-- See schema/schema.sql for complete schema definition
-- This file is for version control and documentation purposes

-- Migration tracking table
CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    version VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    type VARCHAR(10),
    script VARCHAR(1000),
    checksum INTEGER,
    installed_by VARCHAR(100),
    installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER,
    success BOOLEAN
);

-- Record this migration
INSERT INTO schema_migrations (version, description, type, success)
VALUES ('001', 'Initial schema creation', 'SQL', true)
ON CONFLICT DO NOTHING;
