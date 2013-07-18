package org.opendolphin.core.client.comm

import org.opendolphin.LogConfig
import org.opendolphin.core.comm.CreatePresentationModelCommand
import org.opendolphin.core.comm.ValueChangedCommand

import java.util.logging.Level

class BlindCommandBatcherTest extends GroovyTestCase {

    BlindCommandBatcher batcher

    @Override
    protected void setUp() throws Exception {
        batcher = new BlindCommandBatcher()
        batcher.deferMillis = 50
    }

    void testMultipleBlindsAreBatchedNonMerging() {
        doMultipleBlindsAreBatched()
    }
    void testMultipleBlindsAreBatchedMerging() {
        batcher.mergeValueChanges = true
        doMultipleBlindsAreBatched()
    }

    void doMultipleBlindsAreBatched() {
        assert batcher.isEmpty()
        def list = [new CommandAndHandler(), new CommandAndHandler(), new CommandAndHandler()]

        list.each { commandAndHandler -> batcher.batch(commandAndHandler) }
        assert batcher.waitingBatches.val == list
    }

    void testNonBlindForcesBatchNonMerging() {
        doNonBlindForcesBatch()
    }
    void testNonBlindForcesBatchMerging() {
        batcher.mergeValueChanges = true
        doNonBlindForcesBatch()
    }

    void doNonBlindForcesBatch() {
        assert batcher.isEmpty()
        def list = [new CommandAndHandler(), new CommandAndHandler(), new CommandAndHandler()]
        list << new CommandAndHandler(handler: new OnFinishedHandlerAdapter())

        list.each { commandAndHandler -> batcher.batch(commandAndHandler) }
        assert batcher.waitingBatches.val == list[0..2]
        assert batcher.waitingBatches.val == [list[3]]
    }


    void testMaxBatchSizeNonMerging() {
        doMaxBatchSize()
    }
    void testMaxBatchSizeMerging() {
        batcher.mergeValueChanges = true
        doMaxBatchSize()
    }

    void doMaxBatchSize() {
        batcher.maxBatchSize = 4
        def list = [new CommandAndHandler()] * 17

        list.each { commandAndHandler -> batcher.batch(commandAndHandler) }

        4.times {
            assert batcher.waitingBatches.val.size() == 4
        }
        assert batcher.waitingBatches.val.size() == 1
        assert batcher.empty
    }

    void testMergeInOneCommand() {
        LogConfig.logOnLevel(Level.ALL)

        batcher.mergeValueChanges = true
        def list = [
          new CommandAndHandler(command: new ValueChangedCommand(attributeId: 0, oldValue: 0, newValue: 1)),
          new CommandAndHandler(command: new ValueChangedCommand(attributeId: 0, oldValue: 1, newValue: 2)),
          new CommandAndHandler(command: new ValueChangedCommand(attributeId: 0, oldValue: 2, newValue: 3)),
        ]

        list.each { commandAndHandler -> batcher.batch(commandAndHandler) }

        def nextBatch = batcher.waitingBatches.val
        assert nextBatch.size() == 1
        assert nextBatch.first().command.oldValue == 0
        assert nextBatch.first().command.newValue == 3
        assert batcher.empty

    }

    void testMergeCreatePmAfterValueChange() {

        batcher.mergeValueChanges = true
        def list = [
          new CommandAndHandler(command: new ValueChangedCommand(attributeId: 0, oldValue: 0, newValue: 1)),
          new CommandAndHandler(command: new CreatePresentationModelCommand()),
        ]

        list.each { commandAndHandler -> batcher.batch(commandAndHandler) }

        def nextBatch = batcher.waitingBatches.val
        assert nextBatch.size() == 2
        assert nextBatch[0].command instanceof ValueChangedCommand
        assert nextBatch[1].command instanceof CreatePresentationModelCommand
        assert batcher.empty

    }

}
