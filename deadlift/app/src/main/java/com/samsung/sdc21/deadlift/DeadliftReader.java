package com.samsung.sdc21.deadlift;

import androidx.health.services.client.data.DataType;
import androidx.health.services.client.data.ExerciseCapabilities;
import androidx.health.services.client.data.ExerciseType;
import androidx.health.services.client.data.ExerciseTypeCapabilities;

import java.util.Set;

public class DeadliftReader {
    private final ExerciseType exerciseType = ExerciseType.DEADLIFT;

    public Set<DataType> getExerciseCapabilities(ExerciseCapabilities result) throws DeadliftException {
        if (result == null)
            throw new DeadliftException("ExerciseCapabilities is null");
        ExerciseTypeCapabilities exerciseTypeCapabilities =
                result.getExerciseTypeCapabilities(exerciseType);
        @SuppressWarnings("All")
        Set<DataType> dataTypeSet = null;
        dataTypeSet = exerciseTypeCapabilities.getSupportedDataTypes();
        return dataTypeSet;
    }
}
