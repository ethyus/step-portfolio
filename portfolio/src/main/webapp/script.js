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

// driver function
function start(){
    getComment();
    fetchBlobstoreUrlAndShowForm();
}

// Set form action to blobstore URL
function fetchBlobstoreUrlAndShowForm() {
  fetch('/blobstore-upload-url')
    .then((response) => {
      return response.text();
    })
    .then((imageUploadUrl) => {
      const messageForm = document.getElementById('my-form');
      messageForm.action = imageUploadUrl;
      messageForm.classList.remove('hidden');
    });
}

function getComment() {

  const comments = document.getElementById("server");

  // Get nested Json Objects
  const info = fetch("/data")
    .then((info) => info.json())
    .then((info) => {
      if (!("messageInfo" in info)) {
        comments.append(createHeaderElement("No Messages Available"));
      } else if (info["messageInfo"]["history"].length == 0){
        comments.append(createHeaderElement("No Messages Available"));
      } else {
        info["messageInfo"]["history"].forEach((content) => {
        comments.appendChild(createListElement(content.message));
        });
      }

      // Prompt users that inputs were invalid
      if (info["messageInfo"]["error"]){
        nameError = document.getElementById("message_error");
        nameError.style.borderColor = "red";
        nameError.innerText = "Invalid Message - Must include a message"
      }
      if (info["nameInfo"]["error"]){
        messageError = document.getElementById("name_error");
        messageError.style.borderColor = "red";
        messageError.innerText = "Invalid Name - Can only include alphabetial characters"
      }
    })
    .catch(() => {
      let error = "Cannot Display Message";
      comments.append(createHeaderElement(error));
    });

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
