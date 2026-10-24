package com.tpjy.controller;

import com.tpjy.entity.Balance;
import com.tpjy.entity.WeightBox;
import com.tpjy.service.MasterDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 主数据维护（WBS 1.3.7，成员乙）——天平 CRUD。
 * 甲把「天平与模板维护」占位页放在了 /reviewer/balance，本控制器不改甲代码，
 * 用 {@code /admin/masterdata/**} 承载真实的天平维护。
 */
@Controller
@RequestMapping("/admin/masterdata")
public class MasterDataController {

    @Resource
    private MasterDataService masterDataService;

    @GetMapping("")
    public String list(Model model) {
        model.addAttribute("balances", masterDataService.listBalances());
        model.addAttribute("weightBoxes", masterDataService.listWeightBoxes());
        return "admin/masterdata";
    }

    @GetMapping("/edit")
    public String editForm(@RequestParam("id") Long id, Model model) {
        model.addAttribute("balance", masterDataService.getBalance(id));
        return "admin/masterdata-edit";
    }

    @PostMapping("/add")
    public String add(@RequestParam("balanceCode") String balanceCode,
                      @RequestParam("dept") String dept,
                      @RequestParam("uniqueNo") String uniqueNo,
                      @RequestParam("range1") String range1,
                      @RequestParam("range1Precision") String range1Precision,
                      @RequestParam(value = "range2", required = false) String range2,
                      @RequestParam(value = "range2Precision", required = false) String range2Precision,
                      @RequestParam("weightL1") BigDecimal weightL1,
                      @RequestParam("weightL2") BigDecimal weightL2,
                      @RequestParam("weightL3") BigDecimal weightL3) {
        Balance b = new Balance();
        b.setBalanceCode(balanceCode);
        b.setDept(dept);
        b.setUniqueNo(uniqueNo);
        b.setRangeCount(blankToNull(range2) == null ? 1 : 2);
        b.setRange1(range1);
        b.setRange1Precision(range1Precision);
        b.setRange2(blankToNull(range2));
        b.setRange2Precision(blankToNull(range2Precision));
        b.setWeightL1(weightL1);
        b.setWeightL2(weightL2);
        b.setWeightL3(weightL3);
        masterDataService.createBalance(b);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam("id") Long id,
                       @RequestParam("balanceCode") String balanceCode,
                       @RequestParam("dept") String dept,
                       @RequestParam("uniqueNo") String uniqueNo,
                       @RequestParam("range1") String range1,
                       @RequestParam("range1Precision") String range1Precision,
                       @RequestParam(value = "range2", required = false) String range2,
                       @RequestParam(value = "range2Precision", required = false) String range2Precision,
                       @RequestParam("weightL1") BigDecimal weightL1,
                       @RequestParam("weightL2") BigDecimal weightL2,
                       @RequestParam("weightL3") BigDecimal weightL3) {
        Balance b = masterDataService.getBalance(id);
        if (b == null) {
            return "redirect:/admin/masterdata";
        }
        b.setBalanceCode(balanceCode);
        b.setDept(dept);
        b.setUniqueNo(uniqueNo);
        b.setRangeCount(blankToNull(range2) == null ? 1 : 2);
        b.setRange1(range1);
        b.setRange1Precision(range1Precision);
        b.setRange2(blankToNull(range2));
        b.setRange2Precision(blankToNull(range2Precision));
        b.setWeightL1(weightL1);
        b.setWeightL2(weightL2);
        b.setWeightL3(weightL3);
        masterDataService.updateBalance(b);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id) {
        masterDataService.deleteBalance(id);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/weightbox/add")
    public String addWeightBox(@RequestParam("boxNo") String boxNo,
                               @RequestParam(value = "remark", required = false) String remark) {
        WeightBox box = new WeightBox();
        box.setBoxNo(boxNo);
        box.setRemark(remark);
        masterDataService.createWeightBox(box);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/weightbox/delete")
    public String deleteWeightBox(@RequestParam("id") Long id) {
        masterDataService.deleteWeightBox(id);
        return "redirect:/admin/masterdata";
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
