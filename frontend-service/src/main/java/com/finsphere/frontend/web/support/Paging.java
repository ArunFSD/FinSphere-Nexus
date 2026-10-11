package com.finsphere.frontend.web.support;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.api.PageResponse;

/** Page numbers in list addresses ({@code ?page=3}, counted from 0 as the services count them). */
public final class Paging {

    private Paging() {
    }

    /** The page to load for a requested number: negative numbers mean the first page. */
    public static int requested(int page) {
        return Math.max(page, 0);
    }

    /**
     * A redirect to the last page when the requested one is past the end (an old link, or a list that has become
     * shorter), so the list never claims to be empty while it has entries. Null when the requested page exists.
     */
    public static @Nullable String pastTheEnd(String path, int requested, PageResponse<?> loaded) {
        if (requested <= 0 || requested < loaded.totalPages()) {
            return null;
        }
        return "redirect:" + path + ((loaded.totalPages() > 1) ? "?page=" + (loaded.totalPages() - 1) : "");
    }
}
