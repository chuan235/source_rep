// 将Map转为vo对象
            List<ProductionRecordVo> voList = new ArrayList<>();
        List<ProductionRecordVo> newVoList = new ArrayList<>();
        for (Map<String, Object> map : list) {
        voList.add((ProductionRecordVo) BeanUtils.tranMapToBean(map, ProductionRecordVo.class));
        }
        // 根据挤压批分组  便于后续的操作  一对一
        Map<String, ProductionRecordVo> oldProcessLotMap = voList.stream().collect(
        Collectors.toMap(ProductionRecordVo::getProcessLot, vo -> vo));
        // 找出所有的挤压批  对应的outputRecord  sfc  -计算->  发料
        List<String> processLotList = voList.stream().map(ProductionRecordVo::getProcessLot)
        .collect(Collectors.toList());
        List<ProductionRecordVo> outputRecords = recordVoMapper.queryByProcessLot(processLotList);
        // 找出这个挤压批下的所有的sfc
        Map<String, List<ProductionRecordVo>> groupMap = outputRecords.stream().collect(
        Collectors.groupingBy(
        vo -> vo.getProcessLot() + SPLIT_MARK + vo.getSmeltingLot() + SPLIT_MARK + vo.getAlMatnr()));
        // key --  sfclist
        Map<String, List<String>> keySfcMap = new HashMap<>();
        List<String> sfcList = new ArrayList<>();
        for (Map.Entry<String, List<ProductionRecordVo>> entry : groupMap.entrySet()) {
        String key = entry.getKey();
        List<String> sfc = entry.getValue().stream().map(ProductionRecordVo::getSfc).collect(Collectors.toList());
        keySfcMap.put(key, sfc);
        sfcList.addAll(sfc);
        }
        // 根据sfc查询sfc+auart的发料记录
        Map<String, ProductionRecordVo> sfcMap = recordVoMapper.queryBySfc(sfcList).stream().collect(
        Collectors.toMap(ProductionRecordVo::getSfc, vo -> vo));
        // 唯一key --  ProductionRecordVo
        Map<String, ProductionRecordVo> uniqueMap = new HashMap<>();
        // processlot ->  ProductionRecordVo List
        Map<String, List<ProductionRecordVo>> processLotMap = new HashMap<>();
        // 合并groupMap和auartMap
        for (Map.Entry<String, List<String>> sfcEntry : keySfcMap.entrySet()) {
        String key = sfcEntry.getKey();
        List<String> sfcItems = sfcEntry.getValue();
        // 根据工单分类
        Map<String, BigDecimal> auartMap = new HashMap<>();
        for (String item : sfcItems) {
        ProductionRecordVo auartItem = sfcMap.get(item);
        if (ZStringUtils.isNotEmpty(auartItem)) {
        // 一个sfc对应一个工单类型
        String auart = auartItem.getAuart();
        BigDecimal qty = auartItem.getQty();
        BigDecimal oldVal = auartMap.putIfAbsent(auart, qty);
        if (ZStringUtils.isNotEmpty(oldVal)) {
        // 加上原来的
        auartMap.put(auart, NumberUtils.add(qty, oldVal));
        }
        }
        }
        // 构建唯一key
        for (Map.Entry<String, BigDecimal> uniqueEntry : auartMap.entrySet()) {
        String auart = uniqueEntry.getKey();
        ProductionRecordVo outputVo = groupMap.get(key).get(0);
        ProductionRecordVo vo = new ProductionRecordVo();
        String[] keys = key.split(SPLIT_MARK);
        vo.setAuart(auart);
        vo.setUseNum(uniqueEntry.getValue());
        vo.setProcessLot(keys[0]);
        vo.setSmeltingLot(keys[1]);
        vo.setAlMatnr(keys[2]);
        vo.setOlength(outputVo.getOlength());
        vo.setHlength(outputVo.getHlength());
        vo.setTlength(outputVo.getTlength());
        String uniqueKey = key + SPLIT_MARK + auart;
        uniqueMap.put(uniqueKey, vo);
        }
        }
        for (Map.Entry<String, ProductionRecordVo> uniqueEntry : uniqueMap.entrySet()) {
        String processLot = uniqueEntry.getKey().split(SPLIT_MARK)[0];
        ProductionRecordVo value = uniqueEntry.getValue();
        if (ZStringUtils.isNotEmpty(processLotMap.get(processLot))) {
        processLotMap.get(processLot).add(value);
        continue;
        }
        // 初始化
        List<ProductionRecordVo> vos = new ArrayList<>();
        vos.add(value);
        processLotMap.put(processLot, vos);
        }
        for (Map.Entry<String, ProductionRecordVo> oldEntry : oldProcessLotMap.entrySet()) {
        String oldProcessLot = oldEntry.getKey();
        ProductionRecordVo oldVo = oldEntry.getValue();
        // 找processLot在unique中的vo
        List<ProductionRecordVo> newList = processLotMap.get(oldProcessLot);
        if (ZStringUtils.isEmpty(newList)) {
        continue;
        }
        newList.forEach(item -> {
        // 把旧的值写进去
        item.setDeptCode(oldVo.getDeptCode());
        item.setDept(oldVo.getDept());
        item.setCreateTime(oldVo.getCreateTime());
        item.setId(oldVo.getId());
        item.setZbm(oldVo.getZbm());
        item.setJh(oldVo.getJh());
        item.setMouldNo(oldVo.getMouldNo());
        item.setMatnr(oldVo.getMatnr());
        item.setJyNo(oldVo.getJyNo());
        item.setZgly(oldVo.getZgly());
        item.setXmsj(oldVo.getXmsj());
        });
        newVoList.addAll(newList);
        }
        List<String> alMatnrList = newVoList.stream().map(ProductionRecordVo::getAlMatnr).collect(Collectors.toList());
        Map<String, ProductionRecordVo> alMap = recordVoMapper.queryAlByMatnr(alMatnrList).stream()
        .collect(Collectors.toMap(ProductionRecordVo::getAlMatnr, vo -> vo));
        newVoList.forEach(item -> {
        ProductionRecordVo detailVo = recordVoMapper.queryDownReson(item.getId(), item.getAuart());
        ProductionRecordVo alVo = alMap.get(item.getAlMatnr());
        item.setDownReason(detailVo.getDownReason());
        item.setProductionResult(detailVo.getProductionResult());
        item.setBanzu(detailVo.getBanzu());
        item.setZhj(alVo.getZhj());
        item.setZdz(alVo.getZdz());
        item.setZbj(alVo.getZbj());
        item.setZcd(alVo.getZcd());
        item.setZkj(alVo.getZkj());
        if ("0".equals(item.getZkj())) {
        // useNum * ISNULL( b.zdz, 0 ) * b.zcd / 1000
        BigDecimal f1 = NumberUtils.multiply(item.getUseNum(), new BigDecimal(StringUtils.nvl(item.getZdz(), "0")));
        BigDecimal f2 = NumberUtils.multiply(f1, new BigDecimal(item.getZcd()));
        BigDecimal weight = NumberUtils.divide(f2, new BigDecimal(1000));
        item.setWeight(weight.toPlainString());
        } else {
        // ISNULL( b.zdz, 0 ) * (b.zcd / 1000) - (3.14 * (b.zkj/2) * (b.zkj/2) * 2.7 * (b.zcd / 1000)) / 1000)
        BigDecimal dcd = NumberUtils.divide(new BigDecimal(item.getZdz()), new BigDecimal(1000));
        BigDecimal sub1 = NumberUtils.multiply(new BigDecimal(StringUtils.nvl(item.getZdz(), "0")), dcd);
        BigDecimal m1 = NumberUtils.divide(new BigDecimal(StringUtils.nvl(item.getZkj(), "0")), new BigDecimal(2));
        BigDecimal m2 = NumberUtils.multiply(new BigDecimal(3.14), m1);
        BigDecimal m3 = NumberUtils.multiply(m2, m1);
        BigDecimal m4 = NumberUtils.multiply(new BigDecimal(2.7), m3);
        BigDecimal sub2 = NumberUtils.multiply(m4, dcd);
        BigDecimal s = NumberUtils.subtract(sub1, sub2);
        BigDecimal weight = NumberUtils.multiply(s, item.getUseNum());
        item.setWeight(weight.toPlainString());
        }
        });
        list.clear();
        for (ProductionRecordVo vo : newVoList) {
        list.add(BeanUtils.tranBeanToMap(vo));
        }
        System.out.println(list);