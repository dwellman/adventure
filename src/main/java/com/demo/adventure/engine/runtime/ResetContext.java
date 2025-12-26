package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;

import java.util.List;
import java.util.UUID;

record ResetContext(KernelRegistry registry, UUID plotId, UUID playerId, List<Item> inventory) { }
