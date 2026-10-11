package com.finsphere.frontend.web.support;

import java.util.ArrayList;
import java.util.List;

import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.frontend.client.ChitClient;

import org.springframework.stereotype.Component;

import static com.finsphere.frontend.client.BackendResults.data;

/**
 * Every active member, sorted by name, for the member pickers on the slot, loan and payment forms. The API returns
 * at most 100 members per call, so this reads page after page; a single page would silently leave out everyone
 * after the hundredth name.
 */
@Component
public class MemberDirectory {

    static final int PAGE_SIZE = 100;

    /** 5,000 members. Beyond that a drop-down list stops being usable anyway. */
    static final int MAX_PAGES = 50;

    private final ChitClient chitClient;

    public MemberDirectory(ChitClient chitClient) {
        this.chitClient = chitClient;
    }

    /** All members as one page, so the templates can treat it like any other page of members. */
    public PageResponse<MemberResponse> all() {
        List<MemberResponse> members = new ArrayList<>();
        PageResponse<MemberResponse> page;
        int number = 0;
        do {
            page = data(this.chitClient.members(number, PAGE_SIZE));
            members.addAll(page.content());
            number++;
        }
        while (!page.last() && !page.content().isEmpty() && number < MAX_PAGES);
        int pages = members.isEmpty() ? 0 : 1;
        return new PageResponse<>(members, 0, members.size(), members.size(), pages, true, true);
    }
}
