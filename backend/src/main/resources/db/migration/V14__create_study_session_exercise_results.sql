CREATE TABLE study_session_exercise_result (
    session_id UUID PRIMARY KEY REFERENCES study_session (id) ON DELETE CASCADE,
    questions_attempted INTEGER NOT NULL,
    questions_correct INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT study_session_exercise_attempted_positive CHECK (questions_attempted > 0),
    CONSTRAINT study_session_exercise_correct_nonnegative CHECK (questions_correct >= 0),
    CONSTRAINT study_session_exercise_correct_within_attempted CHECK (
        questions_correct <= questions_attempted
    )
);
