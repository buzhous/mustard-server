package org.jeecg.modules.app.controller.base;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.app.entity.FieldElement;
import org.jeecg.modules.app.service.IBaseFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * App 基础数据下发：
 * GET /app/base/fields?platform=ios|android —— 字段字典按端下发（platform 口径与 AppVersion 一致），
 * list 仅含启用且该端已勾选的字段（按 sort 升序，扩展字段排末尾）。
 */
@Slf4j
@RestController
@RequestMapping("/app/base")
@Tag(name = "App 基础数据下发")
public class BaseFieldController {

    @Autowired
    private IBaseFieldService baseFieldService;

    @Operation(summary = "字段字典下发（按端过滤，返回 version + platform + list）")
    @GetMapping(value = "/fields")
    public Result<JSONObject> fields(@RequestParam(name = "platform") String platform) {
        List<FieldElement> list;
        try {
            list = baseFieldService.getActiveFields(platform);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        JSONObject result = new JSONObject(true);
        result.put("version", baseFieldService.getVersion());
        result.put("platform", platform);
        JSONArray arr = new JSONArray();
        for (FieldElement f : list) {
            JSONObject item = new JSONObject(true);
            item.put("element", f.getElement());
            item.put("name", f.getName());
            item.put("fieldKey", f.getFieldKey());
            item.put("inputType", f.getInputType());
            item.put("isRequired", f.getIsRequired());
            item.put("sort", f.getSort());
            arr.add(item);
        }
        result.put("list", arr);
        return Result.OK(result);
    }
}
