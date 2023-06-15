package com.samsung.sdc21.deadlift;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.health.services.client.data.DataType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class DeadliftUnitTest {
    final Set<DataType> readDataTypeSet = new HashSet<>();
    final MockExerciseHelper mockExerciseHelper = new MockExerciseHelper();

    @Test
    public void checkCapabilitiesWhenExerciseCapabilitiesIsNullTest_N() {
        DeadliftReader deadliftReader = new DeadliftReader();
        Throwable exception = assertThrows(DeadliftException.class, () -> deadliftReader.getExerciseCapabilities(null));
        assertEquals("ExerciseCapabilities is null", exception.getMessage());
    }

    @Test
    public void checkCapabilitiesWhenExerciseCapabilitiesIsCorrectTest_P() {
        DeadliftReader deadliftReader = new DeadliftReader();
        try {
            Set<DataType> dataTypeSet = deadliftReader.getExerciseCapabilities(mockExerciseHelper.getCapabilities());
            assertEquals(readDataTypeSet, dataTypeSet);
        } catch (DeadliftException exception) {
            assert (false);
        }
    }

    @Test
    public void setUpdateEventListenerWhenListenerIsNull_N() {
        Deadlift deadlift = new Deadlift();
        Throwable exception = assertThrows(DeadliftException.class, deadlift::startExerciseUpdateListener);
        assertTrue(Objects.equals(exception.getMessage(), "Update is null") || Objects.equals(exception.getMessage(), "Exercise Client is null"));
    }


    @Test
    public void checkUpdateRepCountWhenUpdateIsNullTest_N() {
        Deadlift deadlift = new Deadlift();
        Throwable exception = assertThrows(DeadliftException.class, () -> deadlift.updateRepCount(null));
        assertEquals("Exercise update is null", exception.getMessage());
    }
}
