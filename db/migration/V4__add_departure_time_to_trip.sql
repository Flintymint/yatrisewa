-- Migration: Add departure_time column to trip table
ALTER TABLE trip ADD COLUMN departure_time TIME;
