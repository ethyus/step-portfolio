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
function start () {
  getComment()
  fetchBlobstoreUrlAndShowForm()
}

function startImageGallery () {
  fetchUserStatus()
  fetchBlobstoreUrlForImageGallery()
  getImages()
}

// Set form action to blobstore URL
function fetchBlobstoreUrlAndShowForm () {
  fetch('/blobstore-upload-image-analyzer')
    .then(response => {
      return response.text()
    })
    .then(imageUploadUrl => {
      const messageForm = document.getElementById('my-form')
      messageForm.action = imageUploadUrl
      messageForm.classList.remove('hidden')
    })
}

function fetchUserStatus () {
  fetch('/loginForGallery')
    .then(response => response.json())
    .then(userJson => {
      const status = document.getElementById('logStatus')
      const formStatus = document.getElementById('image-form')
      if (userJson['email'] === 'null') {
        status.setAttribute('href', userJson['loginUrl'])
        status.innerText = 'Login if you want access'
        formStatus.style.display = 'none'
      } else {
        status.setAttribute('href', userJson['logoutUrl'])
        status.innerText = 'Logout'
        formStatus.style.display = 'block'
      }
    })
}

function fetchBlobstoreUrlForImageGallery () {
  fetch('/blobstore-upload-url-gallery')
    .then(response => response.text())
    .then(imageUploadUrl => {
      const messageForm = document.getElementById('my-form-gallery')
      messageForm.action = imageUploadUrl
      messageForm.classList.remove('hidden')
    })
}

function getImages () {
  const imageGallery = document.getElementById('current-images')
  const images = fetch('/newimage')
    .then(images => {
      console.log(images)
      return images.json()
    })
    .then(imageJson => {
      console.log(imageJson)
      imageJson['userImages'].forEach(image => {
        imageGallery.appendChild(createImageElement(image))
      })
    })
}

function createImageElement (image) {
  // Create image warpper div
  const imageWrapper = document.createElement('div')
  imageWrapper.classList.add('img-wrap')

  // Get Blob image from servlet
  const imgElement = document.createElement('img')
  imgElement.setAttribute('src', '/serve?key=' + image['blobKey'])
  imgElement.classList.add('userImage')

  // Add delete button

  const deleteButtonElement = document.createElement('button')
  deleteButtonElement.classList.add('close')
  deleteButtonElement.innerText = 'Delete'
  deleteButtonElement.addEventListener('click', () => {
    deleteImage(image)

    // Remove the task from the DOM.
    imageWrapper.remove()
  })
  imageWrapper.appendChild(imgElement)
  imageWrapper.appendChild(deleteButtonElement)

  return imageWrapper
}

function deleteImage (image) {
  const params = new URLSearchParams()
  params.append('id', image.id)
  fetch('/delete-image', { method: 'POST', body: params })
}

function getComment () {
  const comments = document.getElementById('server')

  // Get nested Json Objects
  const info = fetch('/data')
    .then(info => info.json())
    .then(info => {
      if (!('messageInfo' in info)) {
        comments.append(createHeaderElement('No Messages Available'))
      } else if (info['messageInfo']['history'].length == 0) {
        comments.append(createHeaderElement('No Messages Available'))
      } else {
        info['messageInfo']['history'].forEach(content => {
          comments.appendChild(createListElement(content.message))
        })
      }

      // Prompt users that inputs were invalid
      if (info['messageInfo']['error']) {
        nameError = document.getElementById('message_error')
        nameError.style.borderColor = 'red'
        nameError.innerText = 'Invalid Message - Must include a message'
      }
      if (info['nameInfo']['error']) {
        messageError = document.getElementById('name_error')
        messageError.style.borderColor = 'red'
        messageError.innerText =
          'Invalid Name - Can only include alphabetial characters'
      }
    })
    .catch(() => {
      let error = 'Cannot Display Message'
      comments.append(createHeaderElement(error))
    })
}

function createListElement (text) {
  const liElement = document.createElement('li')
  liElement.innerText = text
  return liElement
}

function createHeaderElement (text) {
  const h2Element = document.createElement('h2')
  h2Element.innerText = text
  return h2Element
}
