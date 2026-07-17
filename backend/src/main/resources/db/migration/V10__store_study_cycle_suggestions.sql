CREATE TABLE study_cycle_suggestion_subject (
    cycle_id UUID NOT NULL REFERENCES study_cycle (id) ON DELETE CASCADE,
    subject_id UUID NOT NULL REFERENCES subject (id),
    input_position INTEGER NOT NULL,
    question_count INTEGER NOT NULL,
    weight INTEGER NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    priority NUMERIC(19, 2) NOT NULL,
    allocated_minutes INTEGER NOT NULL,
    appearance_count INTEGER NOT NULL,
    PRIMARY KEY (cycle_id, subject_id),
    CONSTRAINT study_cycle_suggestion_position_unique UNIQUE (cycle_id, input_position),
    CONSTRAINT study_cycle_suggestion_position_positive CHECK (input_position > 0),
    CONSTRAINT study_cycle_suggestion_question_count_positive CHECK (question_count > 0),
    CONSTRAINT study_cycle_suggestion_weight_positive CHECK (weight > 0),
    CONSTRAINT study_cycle_suggestion_difficulty_valid CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT study_cycle_suggestion_priority_positive CHECK (priority > 0),
    CONSTRAINT study_cycle_suggestion_allocation_valid CHECK (
        allocated_minutes >= 60
        AND MOD(allocated_minutes, 5) = 0
    ),
    CONSTRAINT study_cycle_suggestion_appearances_positive CHECK (appearance_count > 0)
);
