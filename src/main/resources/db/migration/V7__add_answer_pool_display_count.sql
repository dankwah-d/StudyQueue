-- Existing questions keep every option. Set this only to enable a larger distractor pool.
ALTER TABLE questions ADD COLUMN display_option_count INTEGER;
ALTER TABLE questions ADD CONSTRAINT questions_display_option_count_valid
    CHECK (display_option_count IS NULL OR display_option_count >= 2);
