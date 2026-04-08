package com.guild.core.module;

import com.guild.core.module.exception.ModuleLoadException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块加载器 - 使用独立 URLClassLoader 实现模块隔离与热加载
 * <p>
 * 关键设计：
 * <ul>
 *   <li>每个模块使用独立的 ClassLoader，避免类冲突</li>
 *   <li>父 ClassLoader 设为插件主 ClassLoader，共享核心类</li>
 *   <li>支持运行时卸载（关闭 ClassLoader 后 GC 回收）</li>
 *   <li>线程安全：所有操作使用 ConcurrentHashMap 保护</li>
 * </ul>
 */
public class ModuleLoader {

    private final ModuleManager manager;
    private final Map<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();

    public ModuleLoader(ModuleManager manager) {
        this.manager = manager;
    }

    /**
     * 从 jar 文件解析模块描述符
     *
     * @param jarFile 模块 jar 文件
     * @return 解析后的模块描述符
     * @throws ModuleLoadException 解析失败或缺少 module.yml 时抛出
     */
    public ModuleDescriptor parseDescriptor(File jarFile) throws ModuleLoadException {
        try {
            return ModuleDescriptor.fromJar(jarFile);
        } catch (IllegalArgumentException e) {
            throw new ModuleLoadException(e.getMessage());
        } catch (Exception e) {
            throw new ModuleLoadException("无法解析模块描述符: " + e.getMessage(), e);
        }
    }

    /**
     * 实例化模块（使用独立 ClassLoader）
     * <p>
     * 流程：
     * <ol>
     *   <li>创建独立的 URLClassLoader（父级为当前插件的 ClassLoader）</li>
     *   <li>通过 ClassLoader 加载模块主类</li>
     *   <li>校验是否实现了 {@link GuildModule} 接口</li>
     *   <li>通过无参构造函数实例化</li>
     * </ol>
     *
     * @param jarFile  模块 jar 文件
     * @param descriptor 已解析的模块描述符
     * @return 实例化的 GuildModule 对象
     * @throws ModuleLoadException 实例化失败时抛出
     */
    @SuppressWarnings("unchecked")
    public GuildModule instantiateModule(File jarFile, ModuleDescriptor descriptor) throws ModuleLoadException {
        String moduleId = descriptor.getId();
        try {
            // 创建独立的 URLClassLoader
            // 父 ClassLoader 使用插件自身的 ClassLoader，这样模块可以访问核心API类
            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarUrl},
                    getClass().getClassLoader()  // 共享核心类的父加载器
            );

            // 记录此模块的 ClassLoader
            classLoaders.put(moduleId, classLoader);

            // 加载并实例化模块主类
            Class<?> clazz = classLoader.loadClass(descriptor.getMain());

            // 校验是否实现了 GuildModule 接口
            if (!GuildModule.class.isAssignableFrom(clazz)) {
                unloadClassloader(moduleId);  // 加载失败时立即清理 ClassLoader
                throw new ModuleLoadException(
                        "模块主类必须实现 com.guild.core.module.GuildModule 接口: "
                                + descriptor.getMain()
                );
            }

            // 通过无参构造函数实例化
            GuildModule module = (GuildModule) clazz.getDeclaredConstructor().newInstance();
            module.setDescriptor(descriptor);

            return module;

        } catch (ModuleLoadException e) {
            throw e;  // 直接传递已知的模块异常
        } catch (Exception e) {
            unloadClassloader(moduleId);  // 确保清理失败的 ClassLoader
            throw new ModuleLoadException(
                    "模块实例化失败 [" + moduleId + "]: " + e.getMessage(), e
            );
        }
    }

    /**
     * 卸载指定模块的 ClassLoader
     * <p>
     * 关闭 ClassLoader 会释放其持有的资源，使得 GC 可以回收已加载的类。
     * 必须在确保模块不再有任何活动引用后调用。
     *
     * @param moduleId 模块ID
     */
    public void unloadClassloader(String moduleId) {
        URLClassLoader cl = classLoaders.remove(moduleId);
        if (cl != null) {
            try {
                cl.close();  // 触发资源释放
            } catch (IOException ignored) {
                // 关闭失败通常不影响功能，忽略即可
            }
        }
    }

    /**
     * 卸载所有 ClassLoader（插件关闭时调用）
     */
    public void unloadAll() {
        for (String moduleId : new java.util.ArrayList<>(classLoaders.keySet())) {
            unloadClassloader(moduleId);
        }
    }

    /**
     * 检查某模块的 ClassLoader 是否存在
     */
    public boolean hasClassloader(String moduleId) {
        return classLoaders.containsKey(moduleId);
    }

    /**
     * 获取已加载的模块数量
     */
    public int getLoadedCount() {
        return classLoaders.size();
    }
}
