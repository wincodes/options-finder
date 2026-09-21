package com.wincodes.optionsfinder.sources;

import com.wincodes.optionsfinder.models.*;

import java.util.concurrent.CompletableFuture;

public interface OptionSource {

    Source source();

    CompletableFuture<SourceResult> search(SearchRequest request);
}
