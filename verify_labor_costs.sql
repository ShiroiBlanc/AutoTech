-- Verify service_labor_costs table exists and has data
USE AutoTech;

SELECT COUNT(*) as total_service_types FROM service_labor_costs;
SELECT * FROM service_labor_costs ORDER BY service_type;
