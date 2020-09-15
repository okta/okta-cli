/*
 * Copyright 2020-Present Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.cli.common.service;

import com.okta.cli.common.RestException;

import java.io.IOException;

public interface RestClient {

    <T> T get(String url, Class<T> type) throws RestException, IOException;

    <T> T post(String url, Object body, Class<T> responseType) throws RestException, IOException;

    <T> T post(String url, String body, Class<T> responseType) throws RestException, IOException;

}
