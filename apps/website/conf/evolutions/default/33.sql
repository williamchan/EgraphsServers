# --- !Ups

-- Create materialized view
SELECT create_matview('celebrity_categories_mv', 'celebrity_categories_v');
-- Create gin index 
CREATE INDEX celebrity_category_search_idx ON celebrity_categories_mv USING gin(to_tsvector);
