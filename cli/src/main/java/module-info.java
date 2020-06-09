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
module com.okta.cli {
    requires info.picocli;
    requires okta.sdk.api;
    requires okta.sdk.impl;
    requires okta.commons.lang;
    requires okta.config.check;
    requires com.okta.cli.common;
    opens com.okta.cli to info.picocli;
    opens com.okta.cli.commands to info.picocli;
}