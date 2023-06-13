/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stratio.qa.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class ThreadProperty {
    private static final Logger logger = LoggerFactory.getLogger(ThreadProperty.class);

    private static final ThreadLocal<Properties> PROPS = new ThreadLocal<Properties>() {
        protected Properties initialValue() {
            return new Properties();
        }
    };

    /**
     * Default Constructor.
     */
    private ThreadProperty() {
    }

    /**
     * Set a string to share in other class.
     *
     * @param key
     * @param value
     */
    public static void set(String key, String value) {
        PROPS.get().setProperty(key, value);
        logger.debug(String.format("Thread Property set: %s -> %s", key, value));
    }

    /**
     * Get a property shared.
     *
     * @param key
     * @return String
     */
    public static String get(String key) {
        return PROPS.get().getProperty(key);
    }

    /**
     * Whether a property shared is defined or not.
     *
     * @param key
     * @return Boolean
     */
    public static Boolean has(String key) {
        return PROPS.get().getProperty(key) != null;
    }

    /**
     * Remove a property shared
     */
    public static void remove(String key) {
        PROPS.get().remove(key);
    }

}
