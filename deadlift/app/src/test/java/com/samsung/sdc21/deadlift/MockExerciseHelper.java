package com.samsung.sdc21.deadlift;

import androidx.health.services.client.data.ExerciseCapabilities;
import androidx.health.services.client.data.ExerciseType;
import androidx.health.services.client.data.ExerciseTypeCapabilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MockExerciseHelper {
    public ExerciseCapabilities getCapabilities() {
        Map<ExerciseType, ExerciseTypeCapabilities> exerciseMap = new HashMap<>();
        ExerciseTypeCapabilities exerciseTypeCapabilities =
                new ExerciseTypeCapabilities(new HashSet<>(), new HashMap<>(), new HashMap<>(), true, false);
        exerciseMap.put(ExerciseType.DEADLIFT, exerciseTypeCapabilities);
        return new ExerciseCapabilities(exerciseMap);
    }
}
