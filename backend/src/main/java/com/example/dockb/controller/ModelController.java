package com.example.dockb.controller;

import com.example.dockb.common.Result;
import com.example.dockb.config.ai.ModelRegistry;
import com.example.dockb.vo.ModelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型切换 API。
 */
@Slf4j
@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelRegistry modelRegistry;

    public ModelController(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    /**
     * 获取所有可用模型列表，标记当前激活模型。
     */
    @GetMapping
    public Result<List<ModelVO>> listModels() {
        String active = modelRegistry.getActiveModel();
        List<ModelVO> vos = modelRegistry.getModels().stream()
                .map(mi -> {
                    ModelVO vo = new ModelVO();
                    vo.setName(mi.getName());
                    vo.setProvider(mi.getProvider());
                    vo.setDescription(mi.getDescription());
                    vo.setSupportsStream(mi.isSupportsStream());
                    vo.setActive(mi.getName().equals(active));
                    return vo;
                })
                .toList();
        return Result.success(vos);
    }

    /**
     * 切换当前激活模型。
     */
    @PostMapping("/switch")
    public Result<ModelVO> switchModel(@RequestParam String model) {
        try {
            modelRegistry.switchTo(model);
            log.info("[Model] switched to: {}", model);
            ModelVO vo = new ModelVO();
            ModelRegistry.ModelInfo info = modelRegistry.getActiveModelInfo();
            vo.setName(info.getName());
            vo.setProvider(info.getProvider());
            vo.setDescription(info.getDescription());
            vo.setSupportsStream(info.isSupportsStream());
            vo.setActive(true);
            return Result.success(vo);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "模型不存在: " + model);
        }
    }

    /**
     * 获取当前激活模型信息。
     */
    @GetMapping("/active")
    public Result<ModelVO> getActive() {
        ModelRegistry.ModelInfo info = modelRegistry.getActiveModelInfo();
        ModelVO vo = new ModelVO();
        vo.setName(info.getName());
        vo.setProvider(info.getProvider());
        vo.setDescription(info.getDescription());
        vo.setSupportsStream(info.isSupportsStream());
        vo.setActive(true);
        return Result.success(vo);
    }
}
