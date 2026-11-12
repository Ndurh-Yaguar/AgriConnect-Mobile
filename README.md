# AgriConnect
### Grow Smarter, Sell Faster

AgriConnect is an Android application designed to support farmers and agricultural communities by providing a digital platform where users can manage farms, connect with other members of the agricultural community, and participate in buying and selling agricultural products.

The project aims to make agricultural trading and information sharing more accessible by bringing useful agricultural services together in one application.

## Project Overview

AgriConnect is being developed using **Kotlin and Jetpack Compose**, with Firebase providing backend services such as authentication and cloud data storage.

The application follows a farmer-and-customer approach, allowing registered users to participate in the marketplace while also managing their farm-related information.

## Features

The application includes the following main areas:

### User Authentication
- User registration and login.
- Firebase-based authentication.
- User account management.

### Farm Management
- Farm registration and management.
- Farm-related information and details.
- Access to farm management functionality.

### Agricultural Marketplace
- Browse agricultural products.
- Upload products for sale.
- View product listings.
- Support for agricultural trading between users.

### Auctions
- An auction section for agricultural products.
- A platform for users to participate in agricultural buying and selling activities.

### Community Forum
- A space for users to interact.
- Share information and engage with other members of the agricultural community.

### Administration
- Dedicated administrative functionality.
- Management of application-related information and users.

### Image Uploads
- Cloudinary integration for uploading and storing product images.

*Note: Features and functionality may evolve as development continues.*

## Technologies Used

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | Android user interface |
| Android Studio | Development environment |
| Firebase Authentication | User authentication |
| Cloud Firestore | Cloud database |
| Cloud Functions | Backend functions configuration |
| Cloudinary | Image uploads |
| Gradle | Build automation |

## Project Structure

The project is organised into an Android application module and Firebase-related backend configuration.

```
AgriConnect/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/agriconnect/
│           │   ├── Admin.kt
│           │   ├── AuctionScreen.kt
│           │   ├── CloudinaryHelper.kt
│           │   ├── CommunityForum.kt
│           │   ├── DataModels.kt
│           │   ├── FarmManagementScreens.kt
│           │   ├── HomeScreenComponents.kt
│           │   ├── MarketplaceScreen.kt
│           │   ├── ProductUploadScreen.kt
│           │   └── ...
│           │
│           └── res/
│
├── functions/
│   ├── index.js
│   ├── package.json
│   └── package-lock.json
│
├── gradle/
│
├── firestore.rules
├── firestore.indexes.json
├── storage.rules
├── firebase.json
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## ⚙️ Getting Started

Follow these instructions to set up the project on your local machine.

### Prerequisites

Make sure you have the following installed:

- Android Studio.
- JDK 17 or another JDK supported by the project's Gradle version.
- Git.
- An Android emulator or physical Android device.

### 1. Clone the repository

```bash
git clone https://github.com/Ndurh-Yaguar/AgriConnect-Mobile.git
```

Navigate into the project directory:

```bash
cd AgriConnect-Mobile
```

### 2. Open the project

1. Open Android Studio.
2. Select **Open**.
3. Choose the cloned `AgriConnect-Mobile` directory.
4. Allow Gradle to synchronise the project.

### 3. Configure Firebase

AgriConnect uses Firebase services.

To configure Firebase:

1. Create or use an existing Firebase project.
2. Register an Android application using the package name:

   `com.example.agriconnect`

3. Download the `google-services.json` configuration file.
4. Place the file inside the `app/` directory:

```
AgriConnect-Mobile/
└── app/
    └── google-services.json
```

5. Enable the Firebase services required by the application, including Authentication and Cloud Firestore.
6. Configure the appropriate Firestore and Storage security rules.

### 4. Configure Cloudinary

Cloudinary is used for image uploads.

Create a Cloudinary account and configure an upload preset suitable for the application.

Provide the required Cloudinary configuration locally rather than committing private credentials or environment-specific values to the repository.

The application currently contains a placeholder cloud name in `CloudinaryHelper.kt`. Replace it with your local configuration before testing image uploads.

For unsigned uploads, configure the upload preset with appropriate restrictions, including allowed file types and upload size limits.

### 5. Run the application

1. Connect an Android device or start an emulator.
2. Allow Android Studio to finish Gradle synchronisation.
3. Select the `app` run configuration.
4. Click **Run**.

## Security and Configuration

Security and proper configuration are important when working with Firebase and third-party services.

Please observe the following:

- Do not commit service-account private keys or private API secrets.
- Keep local configuration files out of version control.
- Configure Firebase Authentication and database security rules.
- Restrict Cloudinary upload presets appropriately.
- Never rely on client-side checks alone for sensitive administrative operations.

The Firebase security rules included in the repository should be reviewed and tested before deploying the application in a production environment.

## Project Status

AgriConnect is an ongoing Android application development project.

The application brings together farm management, agricultural marketplace functionality, auctions and community interaction. Development and improvements may continue as features are tested and refined.

##  Contributing

Contributions, suggestions and improvements are welcome.

If you would like to contribute:

1. Fork the repository.
2. Create a new branch for your changes.
3. Make and test your changes.
4. Commit your work with a descriptive commit message.
5. Submit a pull request.

## License

Please contact the repository owner before redistributing or reusing the source code.

##  Developer

**AgriConnect – Grow Smarter, Sell Faster**

Developed by **Ndurh-Yaguar**.

GitHub Repository:  
https://github.com/Ndurh-Yaguar/AgriConnect-Mobile
