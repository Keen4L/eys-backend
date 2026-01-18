package com.eys.miniapp.processor;

import com.eys.model.json.SkillEffectConfig;
import com.eys.model.entity.CfgSkill;
import com.eys.model.enums.TriggerMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 技能触发条件处理器
 */
@Component
@RequiredArgsConstructor
public class TriggerProcessor {

    /**
     * 获取触发方式（带默认值）
     */
    public TriggerMode getTriggerMode(CfgSkill skill) {
        SkillEffectConfig config = skill.getSkillConfig();
        if (config == null || config.getTriggerRule() == null) {
            return TriggerMode.DM_PUSH; // 默认为 DM_PUSH
        }
        TriggerMode mode = config.getTriggerRule().getMode();
        return mode != null ? mode : TriggerMode.DM_PUSH;
    }
}
