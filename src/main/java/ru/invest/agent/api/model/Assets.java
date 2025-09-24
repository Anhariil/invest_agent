package ru.invest.agent.api.model;

import lombok.Data;
import ru.invest.agent.api.model.financialAssets.Bond;
import ru.invest.agent.api.model.financialAssets.Share;

import java.util.List;

@Data
public class Assets {

    private List<Share> shares;
    private List<Bond> bonds;
}
