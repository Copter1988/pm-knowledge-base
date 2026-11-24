package com.tpjy.controller;

import com.tpjy.entity.Balance;
import com.tpjy.entity.RecordTemplate;
import com.tpjy.entity.WeightBox;
import com.tpjy.service.MasterDataService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        List<Balance> balances = masterDataService.listBalances();
        List<WeightBox> boxes = masterDataService.listWeightBoxes();
        Map<Long, String> boxNames = new LinkedHashMap<Long, String>();
        Map<Long, Long> boxUsage = new LinkedHashMap<Long, Long>();
        for (WeightBox box : boxes) {
            boxNames.put(box.getId(), box.getBoxNo());
            boxUsage.put(box.getId(), masterDataService.countBalancesUsingWeightBox(box.getId()));
        }
        for (Balance balance : balances) {
            if (balance.getWeightBoxId() != null) {
                balance.setWeightBoxNo(boxNames.get(balance.getWeightBoxId()));
            }
        }
        model.addAttribute("balances", balances);
        model.addAttribute("weightBoxes", boxes);
        model.addAttribute("weightBoxUsage", boxUsage);
        model.addAttribute("templates", masterDataService.listTemplates());
        model.addAttribute("templateFieldChoices", templateFieldChoices());
        return "admin/masterdata";
    }

    @GetMapping("/edit")
    public String editForm(@RequestParam("id") Long id, Model model) {
        model.addAttribute("balance", masterDataService.getBalance(id));
        model.addAttribute("weightBoxes", masterDataService.listWeightBoxes());
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
                      @RequestParam(value = "weightBoxId", required = false) Long weightBoxId,
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
        b.setWeightBoxId(weightBoxId);
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
                       @RequestParam(value = "weightBoxId", required = false) Long weightBoxId,
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
        b.setWeightBoxId(weightBoxId);
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
                               @RequestParam(value = "weightL1", required = false) BigDecimal weightL1,
                               @RequestParam(value = "weightL2", required = false) BigDecimal weightL2,
                               @RequestParam(value = "weightL3", required = false) BigDecimal weightL3,
                               @RequestParam(value = "remark", required = false) String remark) {
        WeightBox box = new WeightBox();
        box.setBoxNo(boxNo);
        box.setWeightL1(weightL1);
        box.setWeightL2(weightL2);
        box.setWeightL3(weightL3);
        box.setRemark(remark);
        masterDataService.createWeightBox(box);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/weightbox/delete")
    public String deleteWeightBox(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            masterDataService.deleteWeightBox(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/masterdata";
    }

    @GetMapping("/weightbox/edit")
    public String editWeightBoxForm(@RequestParam("id") Long id, Model model) {
        model.addAttribute("box", masterDataService.getWeightBox(id));
        return "admin/weightbox-edit";
    }

    @PostMapping("/weightbox/edit")
    public String editWeightBox(@RequestParam("id") Long id,
                                @RequestParam("boxNo") String boxNo,
                                @RequestParam(value = "weightL1", required = false) BigDecimal weightL1,
                                @RequestParam(value = "weightL2", required = false) BigDecimal weightL2,
                                @RequestParam(value = "weightL3", required = false) BigDecimal weightL3,
                                @RequestParam(value = "remark", required = false) String remark) {
        WeightBox box = masterDataService.getWeightBox(id);
        if (box != null) {
            box.setBoxNo(boxNo);
            box.setWeightL1(weightL1);
            box.setWeightL2(weightL2);
            box.setWeightL3(weightL3);
            box.setRemark(remark);
            masterDataService.updateWeightBox(box);
        }
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/template/add")
    public String addTemplate(@RequestParam("fieldKey") String fieldKey,
                              @RequestParam("fieldLabel") String fieldLabel,
                              @RequestParam(value = "sortNo", required = false) Integer sortNo,
                              RedirectAttributes redirectAttributes) {
        if (!templateFieldChoices().containsKey(fieldKey)) {
            redirectAttributes.addFlashAttribute("error", "请选择系统支持的模板字段");
            return "redirect:/admin/masterdata";
        }
        RecordTemplate t = new RecordTemplate();
        t.setFieldKey(fieldKey);
        t.setFieldLabel(fieldLabel);
        t.setVisible(1);
        t.setRequired(0);
        t.setSortNo(sortNo == null ? 0 : sortNo);
        masterDataService.createTemplate(t);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/template/delete")
    public String deleteTemplate(@RequestParam("id") Long id) {
        masterDataService.deleteTemplate(id);
        return "redirect:/admin/masterdata";
    }

    @PostMapping("/template/toggle-visible")
    public String toggleTemplateVisible(@RequestParam("id") Long id) {
        RecordTemplate t = masterDataService.getTemplate(id);
        if (t != null) {
            t.setVisible(t.getVisible() == null || t.getVisible() == 0 ? 1 : 0);
            masterDataService.updateTemplate(t);
        }
        return "redirect:/admin/masterdata";
    }

    @GetMapping("/template/edit")
    public String editTemplateForm(@RequestParam("id") Long id, Model model) {
        RecordTemplate template = masterDataService.getTemplate(id);
        model.addAttribute("template", template);
        model.addAttribute("fieldPurpose", template == null ? null
                : templateFieldChoices().get(template.getFieldKey()));
        return "admin/template-edit";
    }

    @PostMapping("/template/edit")
    public String editTemplate(@RequestParam("id") Long id,
                               @RequestParam("fieldLabel") String fieldLabel,
                               @RequestParam(value = "required", required = false, defaultValue = "0") Integer required,
                               @RequestParam(value = "sortNo", required = false, defaultValue = "0") Integer sortNo) {
        RecordTemplate t = masterDataService.getTemplate(id);
        if (t != null) {
            t.setFieldLabel(fieldLabel);
            t.setRequired(required);
            t.setSortNo(sortNo);
            masterDataService.updateTemplate(t);
        }
        return "redirect:/admin/masterdata";
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static Map<String, String> templateFieldChoices() {
        Map<String, String> choices = new LinkedHashMap<String, String>();
        choices.put("weight_box_no", "砝码盒编号");
        choices.put("range_text", "天平量程与精度");
        choices.put("dept", "使用科室");
        choices.put("unique_no", "设备唯一号");
        choices.put("checker_name", "检验人员");
        choices.put("reviewer_name", "复核人员");
        choices.put("remark", "校验备注");
        return choices;
    }
}
