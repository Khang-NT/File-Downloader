package io.github.khangnt.downloader.worker;

public interface ChunkWorkerListener {
        void onChunkFinished(ChunkWorker worker);
        void onChunkError(ChunkWorker worker, String reason, Throwable throwable);
        void onChunkInterrupted(ChunkWorker worker);
}