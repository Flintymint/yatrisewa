-- Migration: Allow 'teardown' as a valid value for trip_status
ALTER TABLE trip
DROP CONSTRAINT IF EXISTS trip_trip_status_check;
ALTER TABLE trip
ADD CONSTRAINT trip_trip_status_check CHECK (trip_status IN ('departed', 'arrived', 'teardown'));
