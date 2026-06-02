-- Create schema
CREATE SCHEMA IF NOT EXISTS bioracer;

-- Create user table
CREATE TABLE bioracer.user (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create model table
CREATE TABLE bioracer.model (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    front VARCHAR(255) NOT NULL,
    back VARCHAR(255) NOT NULL,
    side VARCHAR(255) NOT NULL,
    gender VARCHAR(50) NOT NULL,
    cover_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create project table
CREATE TABLE bioracer.project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    cover_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES bioracer.user(id) ON DELETE CASCADE
);

-- Create project_images table (for the List<String> images field)
CREATE TABLE bioracer.project_images (
    project_id BIGINT NOT NULL,
    images VARCHAR(255),
    images_key INT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES bioracer.project(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, images_key)
);

-- Create generated_asset table
CREATE TABLE bioracer.generated_asset (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    job_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255),
    pose_id VARCHAR(255),
    category VARCHAR(255),
    secure_url TEXT,
    thumbnail_url TEXT,
    public_id TEXT,
    created_at TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES bioracer.project(id) ON DELETE CASCADE
);

-- Create batch_job table (AssetGenerationJob)
CREATE TABLE bioracer.batch_job (
    job_id VARCHAR(255) PRIMARY KEY,
    run_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    total_count INT DEFAULT 0,
    completed_count INT DEFAULT 0,
    uploaded_count INT DEFAULT 0,
    folder_id BIGINT,
    output_path VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create batch_job_failed_items table
CREATE TABLE bioracer.batch_job_failed_items (
    batch_job_job_id VARCHAR(255) NOT NULL,
    failed_items VARCHAR(255),
    failed_items_key INT NOT NULL,
    FOREIGN KEY (batch_job_job_id) REFERENCES bioracer.batch_job(job_id) ON DELETE CASCADE,
    PRIMARY KEY (batch_job_job_id, failed_items_key)
);

-- Create indexes for performance
CREATE INDEX idx_project_user_id ON bioracer.project(user_id);
CREATE INDEX idx_generated_asset_project_id ON bioracer.generated_asset(project_id);
CREATE INDEX idx_generated_asset_job_id ON bioracer.generated_asset(job_id);
CREATE INDEX idx_user_email ON bioracer.user(email);
