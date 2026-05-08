package com.chronos.chronos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Generic wrapper for paginated responses
// T = the type of item in the list (e.g. JobResponse)
// cursor = the ID of the last item — client sends this back
// to get the next page. This is called "cursor pagination"
// It's better than page numbers because:
// - Works correctly if new items are added between requests
// - Equally fast regardless of which page you're on
@Data
@Builder
public class PagedResponse<T> {

    // The list of items for this page
    private List<T> items;

    // Send this back as ?cursor=xxx to get the next page
    // null means this is the last page — no more items
    private String nextCursor;

    // How many items are in this page
    private int count;

    // Whether there are more pages after this one
    private boolean hasMore;
}
