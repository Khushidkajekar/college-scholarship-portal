CREATE DATABASE IF NOT EXISTS college_scholarship CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE college_scholarship;

CREATE TABLE IF NOT EXISTS users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, full_name VARCHAR(120) NOT NULL, email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT', active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS student_profiles (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL UNIQUE, academic_year VARCHAR(9) NOT NULL,
 course_type ENUM('UG','PG','DIPLOMA') NULL, current_year TINYINT NULL, previous_academic_percentage DECIMAL(5,2) NULL,
 cgpa DECIMAL(4,2) NULL, annual_family_income DECIMAL(13,2) NULL,
 social_category ENUM('GENERAL','OBC','SC','ST','MINORITY') NULL, gender ENUM('MALE','FEMALE','OTHER') NULL,
 state_of_residence VARCHAR(60) NULL, course_name VARCHAR(100) NULL, institution_name VARCHAR(180) NULL,
 institution_approval_status ENUM('APPROVED','PENDING','NOT_APPROVED','NOT_APPLICABLE') NULL,
 admission_route VARCHAR(40) NULL, qualifying_board VARCHAR(120) NULL, qualifying_merit_position INT NULL,
 institution_approval_verified BOOLEAN NOT NULL DEFAULT FALSE, admission_route_verified BOOLEAN NOT NULL DEFAULT FALSE,
 qualifying_conditions_verified BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 CONSTRAINT fk_profile_user FOREIGN KEY(user_id) REFERENCES users(id),
 CONSTRAINT chk_year CHECK(current_year IS NULL OR current_year BETWEEN 1 AND 4),
 CONSTRAINT chk_percentage CHECK(previous_academic_percentage IS NULL OR previous_academic_percentage BETWEEN 0 AND 100),
 CONSTRAINT chk_cgpa CHECK(cgpa IS NULL OR cgpa BETWEEN 0 AND 10),
 CONSTRAINT chk_income CHECK(annual_family_income IS NULL OR annual_family_income >= 0)
);
CREATE TABLE IF NOT EXISTS scholarships (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, scheme_code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(220) NOT NULL,
 administering_authority VARCHAR(220) NOT NULL, description TEXT NOT NULL, academic_year VARCHAR(9) NOT NULL,
 maximum_annual_family_income DECIMAL(13,2) NULL, minimum_previous_academic_percentage DECIMAL(5,2) NULL,
 scholarship_amount DECIMAL(12,2) NULL, application_deadline DATE NULL, official_information_url VARCHAR(500) NULL,
 criteria_verified BOOLEAN NOT NULL DEFAULT FALSE, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS scholarship_courses (scholarship_id BIGINT NOT NULL, course_type VARCHAR(20) NOT NULL, course_name VARCHAR(100) NOT NULL DEFAULT '*', PRIMARY KEY(scholarship_id,course_type,course_name), FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_years (scholarship_id BIGINT NOT NULL, study_year TINYINT NOT NULL, PRIMARY KEY(scholarship_id,study_year), FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_categories (scholarship_id BIGINT NOT NULL, category VARCHAR(30) NOT NULL, PRIMARY KEY(scholarship_id,category), FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_genders (scholarship_id BIGINT NOT NULL, gender VARCHAR(20) NOT NULL, PRIMARY KEY(scholarship_id,gender), FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_states (scholarship_id BIGINT NOT NULL, state_name VARCHAR(60) NOT NULL, PRIMARY KEY(scholarship_id,state_name), FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_rules (id BIGINT PRIMARY KEY AUTO_INCREMENT, scholarship_id BIGINT NOT NULL, rule_code VARCHAR(60) NOT NULL, description VARCHAR(500) NOT NULL, required BOOLEAN NOT NULL DEFAULT TRUE, verified BOOLEAN NOT NULL DEFAULT FALSE, FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_documents (scholarship_id BIGINT NOT NULL, document_name VARCHAR(120) NOT NULL, PRIMARY KEY(scholarship_id,document_name), FOREIGN KEY(scholarship_id) REFERENCES scholarships(id) ON DELETE CASCADE);
CREATE TABLE IF NOT EXISTS scholarship_applications (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, scholarship_id BIGINT NOT NULL,
 application_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, status ENUM('SUBMITTED','UNDER_REVIEW','NEEDS_CORRECTION','APPROVED','REJECTED') NOT NULL DEFAULT 'SUBMITTED',
 student_statement TEXT NULL, admin_remarks TEXT NULL, reviewed_by BIGINT NULL, reviewed_at TIMESTAMP NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uq_application_student_scholarship(user_id,scholarship_id), FOREIGN KEY(user_id) REFERENCES users(id),
 FOREIGN KEY(scholarship_id) REFERENCES scholarships(id), FOREIGN KEY(reviewed_by) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS application_documents (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, application_id BIGINT NOT NULL, document_type VARCHAR(120) NOT NULL,
 original_filename VARCHAR(255) NOT NULL, stored_filename VARCHAR(255) NOT NULL, content_type VARCHAR(120) NOT NULL,
 file_size BIGINT NOT NULL, verification_status ENUM('PENDING','VERIFIED','REJECTED') NOT NULL DEFAULT 'PENDING', verification_remarks VARCHAR(500) NULL,
 uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(application_id) REFERENCES scholarship_applications(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS scholarship_application_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, application_id BIGINT NOT NULL, previous_status VARCHAR(30) NULL,
 new_status VARCHAR(30) NOT NULL, changed_by BIGINT NOT NULL, remarks TEXT NULL, changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(application_id) REFERENCES scholarship_applications(id) ON DELETE CASCADE, FOREIGN KEY(changed_by) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS student_profile_verification_history (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, verified_by BIGINT NOT NULL,
 institution_approval_status VARCHAR(30) NULL, institution_approval_verified BOOLEAN NOT NULL,
 admission_route_verified BOOLEAN NOT NULL, qualifying_conditions_verified BOOLEAN NOT NULL,
 remarks VARCHAR(1000) NOT NULL, changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(verified_by) REFERENCES users(id)
);
CREATE INDEX idx_scholarship_year_active ON scholarships(academic_year,active);
CREATE INDEX idx_app_user_status ON scholarship_applications(user_id,status);
