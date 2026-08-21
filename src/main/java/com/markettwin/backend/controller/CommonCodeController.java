package com.markettwin.backend.controller;

import com.markettwin.backend.dto.response.CommonCodeDto;
import com.markettwin.backend.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 예: GET /api/common-codes?domain=ORG -> [{code: "ORGKT", codeName: "KT"}, ...]
@RestController
@RequestMapping("/api/common-codes")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeService commonCodeService;

    @GetMapping
    public List<CommonCodeDto> getCodesByDomain(@RequestParam String domain) {
        return commonCodeService.getCodesByDomain(domain);
    }
}
