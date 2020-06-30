// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

async function getComment() {
  const response = await fetch("/data");
  const text = await response.json();
  const comments = document.getElementById("server");
  if (text == null) {
    comments.append(createHeaderElement("No Messages Available"));
  } else {
    try {
      text.forEach((message) => {
        comments.appendChild(createListElement(message));
      });
    } catch (err) {
      let error = "Cannot Display Message -> " + String(err);
      comments.append(createHeaderElement(error));
    }
  }
}
function createListElement(text) {
  const liElement = document.createElement("li");
  liElement.innerText = text;
  return liElement;
}
function createHeaderElement(text) {
  const h2Element = document.createElement("h2");
  h2Element.innerText = text;
  return h2Element;
}
