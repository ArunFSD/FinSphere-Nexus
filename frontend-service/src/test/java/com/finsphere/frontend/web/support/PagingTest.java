package com.finsphere.frontend.web.support;

import java.util.List;

import com.finsphere.common.api.PageResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PagingTest {

    @Test
    void negativePagesAreTheFirstPage() {
        assertEquals(0, Paging.requested(-4));
        assertEquals(3, Paging.requested(3));
    }

    @Test
    void pagesPastTheEndGoToTheLastPage() {
        assertEquals("redirect:/members?page=11", Paging.pastTheEnd("/members", 99, page(99, 12)));
        assertEquals("redirect:/schemes", Paging.pastTheEnd("/schemes", 3, page(3, 1)), "one page: no number");
        assertEquals("redirect:/loans", Paging.pastTheEnd("/loans", 2, page(2, 0)), "an empty list");
    }

    @Test
    void existingPagesStay() {
        assertNull(Paging.pastTheEnd("/members", 11, page(11, 12)));
        assertNull(Paging.pastTheEnd("/members", 0, page(0, 0)), "the first page of an empty list");
        assertNull(Paging.pastTheEnd("/members", -1, page(0, 12)));
    }

    private static PageResponse<String> page(int number, int totalPages) {
        List<String> content = (number < totalPages) ? List.of("row") : List.of();
        return new PageResponse<>(content, number, 20, totalPages * 20L, totalPages, number == 0,
                number >= totalPages - 1);
    }
}
