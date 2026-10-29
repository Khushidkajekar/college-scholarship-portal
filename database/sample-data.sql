USE college_scholarship;
-- Fictional demonstration criteria only. Review current official guidelines before relying on them.
INSERT INTO scholarships(scheme_code,name,administering_authority,description,academic_year,maximum_annual_family_income,minimum_previous_academic_percentage,scholarship_amount,application_deadline,official_information_url,criteria_verified,active) VALUES
('SSP-OBC','SSP Post-Matric Scholarship for OBC','Backward Classes Welfare Department, Karnataka','Provisional demonstration criteria; actual eligible courses and conditions require official verification.','2026-27',150000,50,NULL,NULL,'https://ssp.postmatric.karnataka.gov.in/',FALSE,TRUE),
('SSP-SCST','SSP Post-Matric Scholarship for SC/ST','Social Welfare Department, Karnataka','Provisional demonstration criteria; actual eligible courses and conditions require official verification.','2026-27',250000,50,NULL,NULL,'https://ssp.postmatric.karnataka.gov.in/',FALSE,TRUE),
('NSP-CSSS','NSP Central Sector Scheme of Scholarships (CSSS)','Ministry of Education, Government of India','Provisional merit and income conditions only. 85% is not a universal official cutoff; board-specific merit position must be verified.','2026-27',450000,85,NULL,NULL,'https://scholarships.gov.in/',FALSE,TRUE),
('AICTE-PRAGATI','AICTE Pragati Scholarship','All India Council for Technical Education','Provisional criteria only; admission route, approved institution, beneficiary limits and other current conditions require verification.','2026-27',800000,50,NULL,NULL,'https://www.aicte-india.org/schemes/students-development-schemes/Pragati',FALSE,TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name),administering_authority=VALUES(administering_authority),description=VALUES(description),academic_year=VALUES(academic_year),maximum_annual_family_income=VALUES(maximum_annual_family_income),minimum_previous_academic_percentage=VALUES(minimum_previous_academic_percentage),criteria_verified=FALSE;
INSERT IGNORE INTO scholarship_courses(scholarship_id,course_type,course_name) SELECT id,'*','*' FROM scholarships WHERE scheme_code IN ('SSP-OBC','SSP-SCST','NSP-CSSS');
INSERT IGNORE INTO scholarship_courses(scholarship_id,course_type,course_name) SELECT id,'UG','*' FROM scholarships WHERE scheme_code='AICTE-PRAGATI';
INSERT IGNORE INTO scholarship_years(scholarship_id,study_year) SELECT id,n FROM scholarships JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) years WHERE scheme_code IN ('SSP-OBC','SSP-SCST','NSP-CSSS');
INSERT IGNORE INTO scholarship_years(scholarship_id,study_year) SELECT id,1 FROM scholarships WHERE scheme_code='AICTE-PRAGATI';
INSERT IGNORE INTO scholarship_categories(scholarship_id,category) SELECT s.id,c.category FROM scholarships s JOIN (SELECT 'OBC' category UNION SELECT 'GENERAL' UNION SELECT 'SC' UNION SELECT 'ST' UNION SELECT 'MINORITY') c WHERE s.scheme_code='NSP-CSSS';
INSERT IGNORE INTO scholarship_categories(scholarship_id,category) SELECT s.id,c.category FROM scholarships s JOIN (SELECT 'OBC' category UNION SELECT 'SC' UNION SELECT 'ST') c WHERE s.scheme_code IN ('SSP-OBC','SSP-SCST');
INSERT IGNORE INTO scholarship_categories(scholarship_id,category) SELECT id,'*' FROM scholarships WHERE scheme_code='AICTE-PRAGATI';
INSERT IGNORE INTO scholarship_genders(scholarship_id,gender) SELECT s.id,g.gender FROM scholarships s JOIN (SELECT 'MALE' gender UNION SELECT 'FEMALE' UNION SELECT 'OTHER') g WHERE s.scheme_code IN ('SSP-OBC','SSP-SCST','NSP-CSSS');
INSERT IGNORE INTO scholarship_genders(scholarship_id,gender) SELECT id,'FEMALE' FROM scholarships WHERE scheme_code='AICTE-PRAGATI';
INSERT IGNORE INTO scholarship_states(scholarship_id,state_name) SELECT id,'Karnataka' FROM scholarships WHERE scheme_code IN ('SSP-OBC','SSP-SCST');
INSERT IGNORE INTO scholarship_states(scholarship_id,state_name) SELECT id,'*' FROM scholarships WHERE scheme_code IN ('NSP-CSSS','AICTE-PRAGATI');
INSERT INTO scholarship_rules(scholarship_id,rule_code,description,required,verified)
SELECT s.id,r.rule_code,r.description,TRUE,FALSE FROM scholarships s
JOIN (
 SELECT 'NSP-CSSS' scheme_code,'CSSS_BOARD_MERIT' rule_code,'Verify the qualifying examination, board-specific merit position, and current-year rules.' description
 UNION ALL SELECT 'NSP-CSSS','CSSS_OTHER_CONDITIONS','Verify remaining CSSS conditions for this academic year.'
 UNION ALL SELECT 'SSP-OBC','SSP_OBC_COURSE_SCOPE','Verify currently eligible course and study-level coverage for this academic year.'
 UNION ALL SELECT 'SSP-SCST','SSP_SCST_COURSE_SCOPE','Verify currently eligible course and study-level coverage for this academic year.'
 UNION ALL SELECT 'AICTE-PRAGATI','PRAGATI_INSTITUTION_APPROVAL','Verify institution approval and eligibility for the relevant technical course.'
 UNION ALL SELECT 'AICTE-PRAGATI','PRAGATI_ADMISSION_ROUTE','Verify entry-year, lateral-entry, and admission-route rules.'
 UNION ALL SELECT 'AICTE-PRAGATI','PRAGATI_BENEFICIARY_LIMIT','Verify family or beneficiary limits and other current scheme conditions.'
) r ON r.scheme_code=s.scheme_code
WHERE NOT EXISTS(SELECT 1 FROM scholarship_rules old WHERE old.scholarship_id=s.id AND old.rule_code=r.rule_code);
INSERT IGNORE INTO scholarship_documents(scholarship_id,document_name) SELECT id,'Aadhaar or identity proof' FROM scholarships;
INSERT IGNORE INTO scholarship_documents(scholarship_id,document_name) SELECT id,'Income certificate' FROM scholarships;
INSERT IGNORE INTO scholarship_documents(scholarship_id,document_name) SELECT id,'Previous examination marksheet' FROM scholarships;
