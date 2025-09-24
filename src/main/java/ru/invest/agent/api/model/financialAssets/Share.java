package ru.invest.agent.api.model.financialAssets;

import lombok.Data;
import ru.invest.agent.api.constant.AssetsConstant;

@Data
public class Share extends Asset {
    private static final AssetsConstant name = AssetsConstant.SHARE;
}
